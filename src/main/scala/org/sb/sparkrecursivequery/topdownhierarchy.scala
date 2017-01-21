// The code below demonstrates use of graphx and pregel API to do parent-child/ hiearchical / recursive queries.

package org.sb.sparkrecursivequery

import scala.reflect.ClassTag
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkContext,SparkConf}
import scala.util.MurmurHash


object topdownhierarchy {

  def main(args: Array[String]) {

    val conf = new SparkConf(true).setAppName("SparkRecursiveQuery").setMaster("local[3]")
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)

    // sample employe data with columns empid, firstname, lastname, title, mgrid
    // here empid is string but can be int as well
    // here employees EMP006,EMP007,EMP008,EMP009,EMP010 are in a loop - bad data
    val empData = Array(
      ("EMP001", "Bob", "Baker", "CEO", "")
      , ("EMP002", "Jim", "Lake", "CIO", "EMP001")
      , ("EMP003", "Tim", "Gorab", "MGR", "EMP002")
      , ("EMP004", "Rick", "Summer", "MGR", "EMP002")
      , ("EMP005", "Sam", "Cap", "Lead", "EMP004")
      , ("EMP006", "Ron", "Hubb", "Sr.Dev", "EMP008")
      , ("EMP007", "Cathy", "Watson", "Dev", "EMP006")
      , ("EMP008", "Samantha", "Lion", "Dev", "EMP007")
      , ("EMP009", "Jimmy", "Copper", "Dev", "EMP007")
      , ("EMP010", "Shon", "Taylor", "Intern", "EMP009")
    )

    // creating an RDD with 3 partitions to test with sample data above
    val empRDD = sc.parallelize(empData, 3)


    // creating emp graph

    // creating emp vertices
    // Note using MurmurHash for demo purpose .. it is deprecated and should be avoided
    // converting employeeid to instance of Any as the function calcTopLevelHierarcy is designed to take any datatype - employeed id can be string, long, int , etc
    val empVerticesRDD = empRDD.map { emp => (MurmurHash.stringHash(emp._1).toLong, (emp._1.asInstanceOf[Any])) }

    // creating emp edges from mgr to employee
    // assumption is EMPID is unique in RDD empRDD
    // .. so there can be only mgr for an employee on every node can max of only one incoming edge ( edge from mgr to employee)
    val empEdgesRDD = empRDD.filter(v => v._5.length > 0 ).map { emp => Edge(MurmurHash.stringHash(emp._5).toLong, MurmurHash.stringHash(emp._1).toLong, "reportees") }

    // create graph
    val graph = Graph(empVerticesRDD, empEdgesRDD).cache()

    // get the values from calcTopLevelHierarchy graph and join with empRDD to create final RDD
    val empHirearchyRDD = empRDD.map{(emp)=> (MurmurHash.stringHash(emp._1).toLong,(emp))}
      .join(calcTopLevelHierarcy(graph))
      .map{case(id,((emp),(level,root,path,iscyclic,isleaf))) => (emp._1,emp._2,emp._3,emp._4,emp._5,level,root.asInstanceOf[String],path,iscyclic,isleaf)}

    // print RDD
    empHirearchyRDD.collect().foreach(println)


  }



  def vprog(vertexId: VertexId, value: (Long,Int,Any,List[String], Int,String), message: (Long,Int, Any,List[String],Int)): (Long,Int, Any,List[String],Int,String) = {
    if (message._2 < 1)  {  //superstep 0 - initialize
      (value._1,value._2+1,value._3,value._4,value._5,value._6)
    } else if (  message._5 == 1) { // set isCyclic
      (value._1,value._2,value._3, value._4,1,value._6)
    } else  { // set new values
      ( message._1,value._2+1, message._3, message._4 :+ value._6 , value._5,value._6)
    }
  }

  def sendMsg(triplet: EdgeTriplet[(Long,Int,Any,List[String],Int,String), _]): Iterator[(VertexId, (Long,Int,Any,List[String],Int))] = {
    val sourceVertex = triplet.srcAttr
    val destinationVertex = triplet.dstAttr
    // check for icyclic
    if (sourceVertex._1 == triplet.dstId || sourceVertex._1 == destinationVertex._1)
      if (destinationVertex._5==0) { //set iscyclic
        Iterator((triplet.dstId, (sourceVertex._1, sourceVertex._2, sourceVertex._3,sourceVertex._4,  1)))
      } else {
        Iterator.empty
      }
    else { //send new values
      Iterator((triplet.dstId, (sourceVertex._1,sourceVertex._2,sourceVertex._3,sourceVertex._4 ,0  )))
    }
  }

  def mergeMsg(msg1: (Long,Int,Any,List[String],Int), msg2: (Long,Int, Any,List[String],Int)): (Long,Int,Any,List[String],Int) =  {
    // dummy logic not applicable to the data in this usecase
    msg2
  }

  //Graph[(Long,Int,Any,String,Int), ED]
  def calcTopLevelHierarcy[VD: ClassTag, ED: ClassTag](graph: Graph[(Any), ED]): RDD[(Long,(Int,Any,String,Int,Int))]  = {

    val pathSeperator = """/"""

    // initialize id,level,root,path,iscyclic
    val initialMsg = (0L,0,0.asInstanceOf[Any],List("dummy"),0)

    // add three more dummy attributes to the vertices
    val initialGraph = graph.mapVertices((id, v) =>  (id,0,v,List(v.toString),0,v.toString) )

    val sssp = initialGraph.pregel(initialMsg,
      Int.MaxValue,
      EdgeDirection.Out)(
      vprog,
      sendMsg,
      mergeMsg)


    // calculate leaf
    val srcVerterRDD = graph.triplets.map{ x => (x.srcId,1)  }.distinct()

    val withLeafRDD = sssp.vertices.leftOuterJoin(srcVerterRDD)
      .map{ case(id,(a,b)) => (id,(a._2,a._3,a._4.mkString(pathSeperator),a._5, if (b==Some(1)) 0 else 1 )) }

    withLeafRDD

  }


}



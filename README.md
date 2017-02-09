# SparkHierarchicalParentChildQueries
Spark Hierarchical \ Parent-Child \ Recursive Queries

Sample Code which demonstrates Spark Graphx Pregel API to address recursive queries in Spark.

Lots of Stack Overflow Questions on this topic - 

http://stackoverflow.com/questions/38904807/how-to-use-spark-sql-to-do-recursive-query

http://apache-spark-user-list.1001560.n3.nabble.com/Efficient-hierarchical-aggregation-in-Spark-td36.html

http://stackoverflow.com/questions/35464374/recursive-method-call-in-apache-spark (non graphx solution - uses recursion)

.

Use Cases -

1. Financial calculations - Accounts can have one or many sub accounts and those sub accounts can have more sub accounts rolling up to them and so on. It forms like a tree with a root and then nodes and edges. There can be 1 to n trees in the accounting data. Many calculations require sum of all sub accounts rolled up to the topmost parent account.

2. HR Calculations - Some employee calculations are based on Org Chart. Org Chart is again like a tree with nodes and edges but mostly with just one root.

3. Other Usecases - extending existing graphx algorithms like shortestpath to generate paths between nodes.

Most of the databases like Teradata, Oracle, Postgres, MySQL , SQL Server (MSSQL) provide common table expressions (CTE's) and Oracle also provides connect by clause within sql to handle such queries.

SQL Server CTE - https://technet.microsoft.com/en-us/library/ms186243(v=sql.105).aspx

Oracle connect_by clause - https://docs.oracle.com/cd/B19306_01/server.102/b14200/queries003.htm

In Spark, data can be distributed between different paritions on the same node or multiple nodes , it gets difficult to do such queries but with graphx this can be done easily.

Motif in graphframes is another way to do it but it requires to provide depth before hand , not useful in cases when depth\width of tree is unkown. https://graphframes.github.io/user-guide.html#motif-finding

The code demonstrates basic use of graphx and pregel api to take input and generate output as shown below.

--------------- Sample Input Data -------------------------------------

Note: Added incorrect data by making EMP006,EMP007 & EMP008 to form a cyclic graph between them. It's important to identify bad\poor quality data.


.


| EMP_ID | FIRST_NAME | LAST_NAME | TITLE | MGR_ID |
| ------ | ---------- | --------- | ----- | ------ |
|EMP001| Bob| Baker| CEO| |
|EMP002| Jim| Lake| CIO|EMP001|
|EMP003| Tim| Gorab| MGR|EMP002|
|EMP004| Rick| Summer| MGR|EMP002|
|EMP005| Sam| Cap| Lead|EMP004|
|EMP006| Ron| Hubb|Sr.Dev|EMP008|
|EMP007| Cathy| Watson| Dev|EMP006|
|EMP008| Samantha| Lion|Intern|EMP007|


.

.

---------------- Generate Output data with Additional fields -------------------------------------

LEVEL - level of the node with root node as 1.

ROOT - Root Node value for all the non-root nodes.

PATH - Path from the root node to the current node. Also helps identify bad\poor quality data.

IS_CYCLIC - If nodes form a cycle then value is set to 1. Helps Identify bad\poor quality data.

IS_LEAF - Node with no child nodes (nodes with no outgoing edges). Value is set to 1

.



| EMP_ID | FIRST_NAME | LAST_NAME | TITLE | MGR_ID | LEVEL | ROOT | PATH | IS_CYCLIC | IS_LEAF |
| ------ | ---------- | --------- | ----- | ------ | ----- | ---- | ---- | --------- | ------- |
|EMP001| Bob| Baker| CEO| | 1|EMP001| EMP001| 0| 0|
|EMP002| Jim| Lake| CIO|EMP001| 2|EMP001| EMP001/EMP002| 0| 0|
|EMP003| Tim| Gorab| MGR|EMP002| 3|EMP001| EMP001/EMP002/EMP003| 0| 1|
|EMP004| Rick| Summer| MGR|EMP002| 3|EMP001| EMP001/EMP002/EMP004| 0| 0|
|EMP005| Sam| Cap| Lead|EMP004| 4|EMP001|EMP001/EMP002/EMP004/EMP005| 0| 1|
|EMP006| Ron| Hubb|Sr.Dev|EMP008| 3|EMP007| EMP007/EMP008/EMP006| 1| 0|
|EMP007| Cathy| Watson| Dev|EMP006| 3|EMP008| EMP008/EMP006/EMP007| 1| 0|
|EMP008| Samantha| Lion|Intern|EMP007| 3|EMP006| EMP006/EMP007/EMP008| 1| 0|

.

Performance Optimization of Graphx -  https://spark-summit.org/east-2015/experience-and-lessons-learned-for-large-scale-graph-analysis-using-graphx/



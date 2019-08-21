# Hit UCC

Distributable UCC Discovery Algorithm based on Akka

## Motivation
Nowadays, tables in databases contain a lot of data. They do not only have many rows but also many columns. Tables are so large that the data becomes difficult to understand. Most often, structural information or documentation of the data is missing. It is of utmost importance to understand the data in order to work with them and query the data.
Unique column combinations (uniques) are sets of columns with no duplicated rows. If at least one row of a combination contains the same values as another, we speak of a non-unique column combination. Unique column combinations are important when it comes to identifying keys in a data set. They are also used for other tasks such as query optimizations or data cleansing.

Existing state-of-the-art algorithms like HyUCC mainly work on one CPU and are therefore limited to the computing power of this machine. The DUCC algorithm is an exception and has been implemented in a distributed way. In some cases, the discovery of unique column combinations takes so long that the runtime is no longer affordable.

A distributed solution is needed to solve the problem for larger tables.

## Approach

Thomas Bläsius et. al have introduced a two-step, single threaded algorithm 
[ [website](https://hpi.de/friedrich/research/enumdat)
|
[paper](https://hpi.de/friedrich/research/enumdat.html?tx_extbibsonomycsl_publicationlist%5BuserName%5D=puma-friedrich&tx_extbibsonomycsl_publicationlist%5BintraHash%5D=0aecd21152fdb3b41484d610834d7fec&tx_extbibsonomycsl_publicationlist%5BfileName%5D=EfficientlyEnumeratingHittingSetsOfHypergraphsArisingInDataProfiling.pdf&tx_extbibsonomycsl_publicationlist%5Bcontroller%5D=Document&cHash=5903f6c0f9af33a41f2c0095bc15c28a) ]
that finds all unique column combinations, whose search space is not represented by a lattice (like traditional algorithms and thus better for distribution). In the first step, all minimal difference sets are formed from the data. A difference set indicates at which positions (columns) the two rows have different values. For this step, all rows are compared pairwise to form difference sets and the resulting sets are then compared in pairs to find all minimal sets. In the second step, unique column combinations are formed from the minimal difference sets by tree search based on the difference sets.
The slowest step in this algorithm is the discovery of the difference sets and, therefore, the one with the highest potential for parallelization.

Xu Chu et al. introduced a way to distribute data for an algorithm that compares the columns pairwise [ [publication](https://dl.acm.org/citation.cfm?id=2983203) ]. They called it the Triangle Distribution Strategy. Since the above presented algorithm also compares the columns pairwise in the first step and the actual comparison operation is interchangeable, we will use this strategy for the first step.

## Distribution Approach

We have decided not to build a master-slave architecture. This is easier to develop, but brings performance and scaling problems with it. The result is a peer-to-peer network that regulates itself. There is no master that takes over the communication and therefore has too little load or can function as a single point of failure.

There are exactly 2 synchronization steps in the entire algorithm. A node reads the data and distributes it to all other nodes and itself. From then on, each node processes its tasks independently. This is most of the algorithm timewise. When the nodes are finished, there is a large synchronization point. Once this point is reached it goes into step two. Here each node has all necessary data to work autonomously or to communicate with other nodes. Finally there is the last synchronization to finish the algorithm and distribute the result to all.

TODO: When nodes are finished early or new ones are added, work-stealing concepts are used.

## Requirements

In order to build and execute the code, you will need Java 8 and Maven.
To make sure that your project is set up correctly in an IDE, you can run the tests in the `hit_ucc/src/test/java` folder. If you are operating from a command line instead, run `mvn test` in the folder with `pom.xml` file.

## Execution instructions

### sbt - docker
- `docker:stage` - create dockerfile
- `docker:publishLocal` - create docker container 

Just run the main class `hit_ucc.HitUCCApp`, respectively, from within your IDE or from the command line. The app will then print an overview of the different possible parameters. Append parameters of your choice to the run configuration in your IDE or to your command line call, as exemplified below:
* Parameters to start a peer to peer system with two local workers: `peer --workers 2 -i bridges.csv`
* To start a peer to peer system with custom data duplication factor: `peer --workers 2 -i bridges.csv -ddf 8`
* More complicated csv file with header and exotic delimiter: `peer --workers 4 -i flight_1k.csv --csvDelimiter ; --csvSkipHeader`
* If every null value should be equals to each other null value just add: `peer --workers 2 -i bridges.csv --nullEqualsNull`

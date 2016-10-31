HS algorithm for leader election in synchronous rings:

This is a simple simulator that simulates a synchronous distributed system using multithreading.
There are n+1 threads in the system: Each of the n processes is simulated by one thread and there is one master thread. The master thread informs all threads when one round starts. Each thread simulates one process, before it can begin round x, waits for the master thread for a "go ahead" signal for round x. The master thread gives the signal to start round x to the threads only if the master thread is sure that all the n threads (simulating n processes) have completed their previous round (round x-1).

Input: 
The input for this problem consists of two parts; part 1 is n (the number of processes of the distributed system which is equal to the number of threads to be created), and part 2 is one array id[n] of size n; the ith element of this array gives the unique id of the i
th process (simulated by the ith thread). 

The master thread reads these two inputs and then spawns n threads. All links are bidirectional and there is
an undirected link between processes j-1 (mod n) and j and one undirected link between processes j and j+1 (mod n).

Output:
All processes must terminate after finding the leader’s id.

Steps:

1) Please make sure that input.txt file is in the same folder as the project
2) If running from command line execute: javac HSMaster.java
3) Then, java HSMaster

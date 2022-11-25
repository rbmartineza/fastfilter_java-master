# Supporting Dynamic Insertions in Xor and Binary Fuse Filters with the Integrated XOR/BIF-Bloom Filter

The following repository contains the directories with the code used to get the results of the paper: 

Roberto Martínez, Pedro Reviriego and David Larrabeiti "Supporting Dynamic Insertions in Xor and Binary Fuse Filters with the Integrated XOR/BIF-Bloom Filter"

Both implementations use an auxiliary Bloom filter to allow dynamic insertions integrated into a Xor/Binary fuse filter.

The code, written in java, is based on https://github.com/FastFilter/fastfilter_java . The IXOR and IBIF filters with their respective test files have been added to the code.

In the path fastfilter_java-master/fastfilter/src/main/java/org/fastfilter/xor/ added the files with the code for IXOR and IBIF as Xor8Bloom and XorBinaryFuse8Bloom as well as a naive, serial implementation of each one.

In the path fastfilter_java-master/tree/main/fastfilter/src/test/java/org/fastfilter, the tests for 100 million elements were added. 

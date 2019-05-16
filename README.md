# big-sorter
<a href="https://travis-ci.org/davidmoten/big-sorter"><img src="https://travis-ci.org/davidmoten/big-sorter.svg"/></a><br/>
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.davidmoten/big-sorter/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/com.github.davidmoten/big-sorter)<br/>
[![codecov](https://codecov.io/gh/davidmoten/big-sorter/branch/master/graph/badge.svg)](https://codecov.io/gh/davidmoten/big-sorter)<br/>

Sorts very large files (or `InputStream`s) by splitting to many intermediate small sorted files and merging.

## Features

* Easy to use builder
* Single threaded
* Sorts one billion integers from a file to a new file in 444s 
* Serialization helpers for lines of strings, Java IO Serialization, DataInputStream based, and fixed length records 
* Serialization customizable

## Algorithm
A large file or `InputStream` of records is sorted by:
* splitting the whole file into smaller segments according to `maxItemsPerFile`
* each segment is sorted in memory and then written to a file
* the segment files are then merged in groups according to `maxFilesPerMerge`
* the merged files are repeatedly merged in groups until only one file remains (with all of the sorted entries)
* the merge step uses a Min Heap (PriorityQueue) for efficiency

## Getting started
TODO

## Example using Java IO Serialization

```java
File in = ...
File out = ...
Sorter 
  .serializer(Serializer.<Long>java()) 
  .comparator(Comparator.naturalOrder()) 
  .input(in) 
  .maxFilesPerMerge(100) 
  .maxItemsPerFile(100000) 
  .output(out) 
  .sort();

```

## Example using a custom Serializer
Here's a serializer for a simple format using one 4 byte signed integer per record:

```java
Serializer<Integer> serializer = new DataSerializer<Integer>() {

    @Override
    public Integer read(DataInputStream dis) throws IOException {
        try {
           return dis.readInt();
        } catch (EOFException e) {
            return null;
        }
    }

    @Override
    public void write(DataOutputStream dos, Integer value) throws IOException {
        dos.writeInt(value);
    }
};
      
Sorter 
  .serializer(serializer) 
  .comparator((x, y) -> Integer.compare(x, y)) 
  .input(in) 
  .output(out) 
  .sort();
``` 
## Benchmarks

```
10^3 integers sorted in 0.004s
10^4 integers sorted in 0.013s
10^5 integers sorted in 0.064s
10^6 integers sorted in 0.605s
10^7 integers sorted in 3.166s
10^8 integers sorted in 35.978s
10^9 integers sorted in 444.549s
```

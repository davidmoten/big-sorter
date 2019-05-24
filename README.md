# big-sorter
<a href="https://travis-ci.org/davidmoten/big-sorter"><img src="https://travis-ci.org/davidmoten/big-sorter.svg"/></a><br/>
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.davidmoten/big-sorter/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/com.github.davidmoten/big-sorter)<br/>
[![codecov](https://codecov.io/gh/davidmoten/big-sorter/branch/master/graph/badge.svg)](https://codecov.io/gh/davidmoten/big-sorter)<br/>

Sorts very large files (or `InputStream`s) by splitting to many intermediate small sorted files and merging.

Status: *deployed to Maven Central*

## Features

* Easy to use builder
* Single threaded
* Sorts one billion integers from a file to a new file in 444s 
* Serialization helpers for 
  * lines of strings
  * Java IO Serialization
  * DataInputStream base
  * fixed length records 
  * CSV
* Serialization customizable

## Algorithm
A large file or `InputStream` of records is sorted by:
* splitting the whole file into smaller segments according to `maxItemsPerFile`
* each segment is sorted in memory and then written to a file
* the segment files are then merged in groups according to `maxFilesPerMerge`
* the merged files are repeatedly merged in groups until only one file remains (with all of the sorted entries)
* Note that merged groups are merged with merged groups to ensure that we don't start approaching insertion sort computational complexity (O(n<sup>2</sup>).
* the merge step uses a Min Heap (`PriorityQueue`) for efficiency

## Getting started
Add this dependency to your maven pom.xml:
```xml
<dependency>
    <groupId>com.github.davidmoten</groupId>
    <artifactId>big-sorter</artifactId>
    <version>VERSION_HERE</version>
</dependency>
```
If you want to sort csv add this extra dependency:
```xml
<dependency>
    <groupId>com.github.davidmoten</groupId>
    <artifactId>commons-csv</artifactId>
    <version>1.6.002</version>
</dependency>
```
The above csv dependency will be switched out for Apache [commons-csv](https://github.com/apache/commons-csv) once [CVS-239](https://issues.apache.org/jira/browse/CSV-239) is resolved (should be sorted in commons-csv 1.7).

## Serialization
To read records from files or InputStreams and to write records to files we need to specify the *serialization* method to use.

### Example for sorting text lines

```java
File in = ...
File out = ...
Sorter
  // set both serializer and natural comparator
  .serializerTextUtf8()
  .input(in)
  .maxFilesPerMerge(100) 
  .maxItemsPerFile(100000) 
  .output(out)
  .sort();
```

or for a different character set and in reverse order:

```java
Sorter
  // set both serializer and natural comparator
  .serializerText(charset)
  .comparator(Comparator.reverseOrder())
  .input(in)
  .output(out)
  .sort();
```
### Example for sorting CSV
Given the CSV file below, we will sort on the second column (the "number" column):
```
name,number,cost
WIPER BLADE,35,12.55
ALLEN KEY 5MM,27,3.80
```

```java
Serializer<CSVRecord> serializer = Serializer.csv(
        CSVFormat.DEFAULT.withFirstRecordAsHeader().withRecordSeparator("\n"),
        StandardCharsets.UTF_8);
Comparator<CSVRecord> comparator = (x, y) -> {
    int a = Integer.parseInt(x.get("number"));
    int b = Integer.parseInt(y.get("number"));
    return Integer.compare(a, b);
};
Sorter 
  .serializer(serializer) 
  .comparator(comparator) 
  .input(inputFile) 
  .output(outputFile)
  .sort();
```
The result is:
```
name,number,cost
ALLEN KEY 5MM,27,3.80
WIPER BLADE,35,12.55
```

### Example using Java IO Serialization
If each record has been written to the input file using `ObjectOutputStream` then we specify the *java()* Serializer:

```java
Sorter 
  .serializer(Serializer.<Long>java()) 
  .comparator(Comparator.naturalOrder()) 
  .input(in) 
  .output(out) 
  .sort();

```
### Example using the DataSerializer helper
If you would like to serializer/deserialize your objects using `DataOutputStream`/`DataInputStream` then extend the `DataSerializer` class as below. This is a good option for many binary formats. 

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
### Custom serialization
To fully do your own thing you need to implement the `Serializer` interface.

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

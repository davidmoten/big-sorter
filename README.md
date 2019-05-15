# big-sorter
Sorts very large files (or `InputStream`s) by splitting to many intermediate small sorted files and merging.

## Features

* Easy to use builder
* Single threaded
* Sorts one billion integers from a file to a new file in 444s 
* Serialization helpers for lines of strings, Java IO Serialization, DataInputStream based, and fixed length records 
* Serialization customizable

## Getting started

## Example

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

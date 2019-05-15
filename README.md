# big-sorter
Sorts very large files (or `InputStream`s) by splitting to many intermediate small sorted files and merging.

## Features

* Easy to use builder
* Single threaded
* Sorts one billion integers from a file to a new file in 444s 
* Serialization helpers for lines of strings, Java IO Serialization, DataInputStream based, and fixed length records 
* Serialization customizable

## Getting started

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
  .serializer(serializer) //
  .comparator((x, y) -> Integer.compare(x, y)) //
  .input(in) //
  .output(out) //
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

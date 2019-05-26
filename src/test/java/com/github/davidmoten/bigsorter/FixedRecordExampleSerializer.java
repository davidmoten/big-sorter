package com.github.davidmoten.bigsorter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.github.davidmoten.bigsorter.FixedRecordExampleSerializer.Person;

public class FixedRecordExampleSerializer extends DataSerializer<Person> {

    public static final class Person {
        final String name;
        final int heightCm;

        public Person(String name, int heightCm) {
            super();
            this.name = name;
            this.heightCm = heightCm;
        }
    }

    @Override
    public Person read(DataInputStream dis) throws IOException {
        short length;
        try {
            length= dis.readShort();
        } catch (EOFException e) {
            return null;
        }
        byte[] bytes = new byte[length];
        dis.readFully(bytes);
        String name = new String(bytes, StandardCharsets.UTF_8);
        int heightCm = dis.readInt();
        return new Person(name, heightCm);
    }

    @Override
    public void write(DataOutputStream dos, Person p) throws IOException {
        dos.writeShort((short) p.name.length());
        dos.write(p.name.getBytes(StandardCharsets.UTF_8));
        dos.writeInt(p.heightCm);
    }

}

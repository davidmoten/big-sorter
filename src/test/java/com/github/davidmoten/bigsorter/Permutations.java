package com.github.davidmoten.bigsorter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.github.davidmoten.guavamini.Lists;

/**
 * Not related to big sorter, just a demo for issue #6.
 */
public class Permutations {

    public static <T> void forEach(List<List<T>> lists,
            Consumer<? super List<? extends T>> consumer) {
        List<Integer> indexes = new ArrayList<>();
        lists.forEach(x -> indexes.add(0));
        report(lists, consumer, indexes);
        while (true) {
            int i = indexes.size() - 1;
            while (i >= 0 && indexes.get(i) == lists.get(i).size() - 1) {
                indexes.set(i, 0);
                i--;
            }
            if (i < 0) {
                return;
            } else {
                indexes.set(i, indexes.get(i) + 1);
            }
            report(lists, consumer, indexes);
        }
    }

    private static <T> void report(List<List<T>> lists,
            Consumer<? super List<? extends T>> consumer, List<Integer> indexes) {
        List<T> output = new ArrayList<>();
        for (int j = 0; j < indexes.size(); j++) {
            output.add(lists.get(j).get(indexes.get(j)));
        }
        consumer.accept(output);
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        List<String> list1 = Lists.newArrayList("aardvaark", "apple", "azalea");
        List<String> list2 = Lists.newArrayList("bat", "bother", "butter");
        List<String> list3 = Lists.newArrayList("cat", "core");

        List<List<String>> lists = Lists.newArrayList(list1, list2, list3);
        Permutations.forEach(lists, System.out::println);
    }

}

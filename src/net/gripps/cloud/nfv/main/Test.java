package net.gripps.cloud.nfv.main;


import java.util.*;


public class Test {
    public static void main(String[] args) {
        //通常のラムダ式
        Function f = (String name) -> {return "hello" + name;};
        //引数の型を省略したバージョン
        Function f2 = (name) -> {return "hello2"+ name;};
        //処理がreturnの1つのみの場合は，カッコ{}とreturnを省略できる．
        Function f3 = (name) -> "hello3" + name;
        //さらに，引数が一つのみの場合は，引数のカッコ()も省略できる．
        Function f4 = name -> "hello4" + name;
        System.out.println(f.test("lambda"));
        List<String> list = new ArrayList<>(
                Arrays.asList("test", "test2", "test3")
        );
        list.replaceAll(str -> {
            if(str.startsWith("t")){
                return "*" + str;
            }else{
                return str;
            }
        });
        System.out.println(list);

        /**
         * Stream API
         */
        List<Integer> intList = Arrays.asList(1, 2, 3, 4, 5);
        intList.stream()
                .filter(i -> i % 2 == 0 ) //フィルタして集める（trueのものだけを集める）
                .forEach(i -> System.out.println(i)); //フィルタした要素全てに対する処理

        List<Integer> intList2 = Arrays.asList(1, 2, 3, 4, 5);
        intList2.stream()
                .map(i -> i * 2)  //すべての要素に対する処理（変換処理）
                .forEach(i -> System.out.println(i)); //mapの結果の要素全てに対する処理

        List<Integer> intList3 = Arrays.asList(1, 2, 3, 4, 5);
        intList3.stream()
                .sorted((a,b) -> Integer.compare(a, b)) // 昇順とする．降順の場合は，compare(a,b) * -1とする．
                .forEach(i -> System.out.println(i));

    }
    private  interface Function{
        String test(String name);}
}

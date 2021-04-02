package ru.devvault;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Общий класс для объектов
 */
abstract class MyObject {
    private final String fieldValue;

    protected MyObject(String fieldValue) {
        this.fieldValue = fieldValue;
    }

    public String getFieldValue() { return fieldValue; }
}

/**
 * Нативный объект = Всё по дефолту
 */
class NativeObject extends MyObject {
    public NativeObject(String fieldValue) { super(fieldValue); }
}

/**
 * Коллизионный объект = Хеш всегда одинаковый
 */
class CollidedObject extends MyObject {
    public CollidedObject(String fieldValue) { super(fieldValue); }

    @Override
    public int hashCode() {
        return 1;
    }

    @Override
    public boolean equals(Object obj)
    {
        if(obj == null || obj.getClass() != this.getClass())
            return false;

        if(this == obj)
            return true;

        MyObject myObject = (MyObject) obj;

        return (myObject.getFieldValue().equals(this.getFieldValue()));
    }
}

/**
 * Нормальный объект = нормально реализованы hashCode и equals
 */
class NormalObject extends MyObject {
    public NormalObject(String fieldValue) { super(fieldValue); }

    @Override
    public boolean equals(Object obj)
    {
        if(obj == null || obj.getClass() != this.getClass())
            return false;

        if(this == obj)
            return true;

        MyObject myObject = (MyObject) obj;

        return (myObject.getFieldValue().equals(this.getFieldValue()));
    }

    @Override
    public int hashCode() {
        final int prime = 99654;
        int result = 1;
        result = prime * result
            + ((getFieldValue() == null) ? 0 : getFieldValue().hashCode());
        return result;
    }
}

/**
 * Класс-заполнитель тестовой мапы
 * @param <T>
 */
class MapFiller<T extends MyObject> implements Callable<MapFiller<? extends MyObject>> {
    private static final int MAP_LENGTH = 30000;

    private final String type;
    private final Class<T> clazz;

    public MapFiller(String type, Class<T> clazz) {
        this.type = type; this.clazz = clazz;
    }

    private final HashMap<T, T> map = new HashMap<>();
    private T first = null;
    private T mid = null;
    private T last = null;

    /**
     * Тип мапы
     * @return
     */
    public String getType() {
        return type;
    }

    /**
     * Первый элемент
     * @return
     */
    public T getFirst() {
        return first;
    }

    /**
     * Средний элемент
     * @return
     */
    public T getMid() {
        return mid;
    }

    /**
     * Последний элемент
     * @return
     */
    public T getLast() {
        return last;
    }

    /**
     * Получение мапы
     * @return
     */
    public Map<T, T> getMap() {
        return map;
    }

    /**
     * Заполнение мапы
     * @return
     * @throws Exception
     */
    @Override
    public MapFiller<T> call() throws Exception {
        Main.printTime(type + " fill takes", () -> {
            for(int i = 0; i < MAP_LENGTH; i++) {
                T obj = null;
                try {
                    obj = clazz.getConstructor(String.class).newInstance(type.toLowerCase() + "Object_" + i);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    e.printStackTrace();
                }
                map.put(obj, obj);

                if (i == 0) {
                    first = obj;
                } else if (i == (MAP_LENGTH / 2)) {
                    mid = obj;
                } else if ( i == (MAP_LENGTH - 1)) {
                    last = obj;
                }
            }
        });

        return this;
    }
}

public class Main {

    /**
     * Вывод времени выполнения
     * @param taskName
     * @param runnable
     */
    public static void printTime(String taskName, Runnable runnable) {
        long startTime = System.currentTimeMillis();
        runnable.run();
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        System.out.println(" --- " + taskName + " takes: " + duration + " ms");
    }

    /**
     * Вывод произоводительности получения элемента из начала/середины/конца
     * @param mapFiller
     * @param type
     * @param <T>
     */
    public static <T extends MyObject> void printQueryPerformance(MapFiller<T> mapFiller) {
        if (mapFiller != null) {
            printTime(mapFiller.getType() + " first query", () -> System.out.println("First element: " + mapFiller.getMap().get(mapFiller.getFirst())));
            printTime(mapFiller.getType() + " mid query", () -> System.out.println("Mid element: " + mapFiller.getMap().get(mapFiller.getMid())));
            printTime(mapFiller.getType() + " last query", () -> System.out.println("Last element: " + mapFiller.getMap().get(mapFiller.getLast())));
        }
    }

    /**
     * Вывод производительности добавления элемента в конец
     * @param item
     * @param mapFiller
     * @param type
     * @param <T>
     */
    public static <T extends MyObject> void printPutPerformance(MapFiller<T> mapFiller) {
        if (mapFiller != null) {
            try {
                T newObj = (T) mapFiller.getFirst().getClass().getConstructor(String.class).newInstance(mapFiller.getType().toLowerCase() + "Object_1000");
                printTime(mapFiller.getType() + " put element", () -> System.out.println(mapFiller.getType() + " put element: " + mapFiller.getMap().put(newObj, newObj)));
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Вывод производительности удаления первого элемента
     * @param mapFiller
     * @param type
     * @param <T>
     */
    public static <T extends MyObject> void printRemovePerformance(MapFiller<T> mapFiller) {
        if (mapFiller != null) {
            printTime(mapFiller.getType() + " remove first element", () -> System.out.println(mapFiller.getType() + " remove element: " + mapFiller.getMap().remove(mapFiller.getFirst())));
        }
    }

    public static void main(String[] args) {
        ExecutorService es = Executors.newFixedThreadPool(3);

        MapFiller<NativeObject> nativeMapFiller = new MapFiller<>("Native", NativeObject.class);
        MapFiller<CollidedObject> collidedMapFiller = new MapFiller<>("Collided", CollidedObject.class);
        MapFiller<NormalObject> normalMapFiller = new MapFiller<>("Normal", NormalObject.class);

        try {
            es.invokeAll(Arrays.asList(nativeMapFiller, collidedMapFiller, normalMapFiller));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            es.shutdown();
        }

        // Перформанс получения
        printQueryPerformance(nativeMapFiller);
        printQueryPerformance(collidedMapFiller);
        printQueryPerformance(normalMapFiller);

        // Перформанс вставки
        printPutPerformance(nativeMapFiller);
        printPutPerformance(collidedMapFiller);
        printPutPerformance(normalMapFiller);

        // Перформанс удаления
        printRemovePerformance(nativeMapFiller);
        printRemovePerformance(collidedMapFiller);
        printRemovePerformance(normalMapFiller);
    }
}

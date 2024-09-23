package fr.wonder.argparser;

import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.*;

public class TestUtils {

    // Must only be called by a method in this class called by a method somewhere else
    private static Class<?> getCallerClass() {
        String className = Thread.currentThread().getStackTrace()[3].getClassName();
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unreachable class");
        }
    }

    public static void runWithInstance(boolean expectSuccess, String args) {
        try {
            System.out.println(">>> Running '" + args + "'");
            Class<?> entryClass = getCallerClass();
            Object entryClassInstance = entryClass.getConstructor().newInstance();
            ArgParser argParser = new ArgParser(entryClass.getSimpleName(), entryClass, entryClassInstance);
            boolean success = argParser.run(args);
            assertEquals(success, expectSuccess);
        } catch (InvalidDeclarationError | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static void run(boolean expectSuccess, String args) {
        Class<?> entryClass = getCallerClass();
        try {
            System.out.println(">>> Running '" + args + "'");
            boolean success = new ArgParser(entryClass.getSimpleName(), entryClass).run(args);
            assertEquals(success, expectSuccess);
        } catch (InvalidDeclarationError e) {
            throw new RuntimeException(e);
        }
    }
}

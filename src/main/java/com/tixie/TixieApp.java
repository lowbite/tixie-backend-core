package com.tixie;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class TixieApp {

    public static void main(String... args) {
        Quarkus.run(args);
    }
}

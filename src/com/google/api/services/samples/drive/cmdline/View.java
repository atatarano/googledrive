package com.google.api.services.samples.drive.cmdline;

import java.io.PrintStream;

public class View
{
  static void header1(String name)
  {
    System.out.println();
    System.out.println("================== " + name + " ==================");
    System.out.println();
  }
  
  static void header2(String name)
  {
    System.out.println();
    System.out.println("~~~~~~~~~~~~~~~~~~ " + name + " ~~~~~~~~~~~~~~~~~~");
    System.out.println();
  }
}

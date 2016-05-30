/*
 * Copyright (c) 2016 Anton Mazhurin to present
 */
package com.greymemory.nab;

/**
 *
 * @author AMazhurin
 */
public class NAB_test {
     public static void main(String[] args)  {
        System.out.println("Greymemory anomaly detector. 2016. \nNumenta NAB test. ");
        
        Labels labels = new Labels();
        labels.Read("../nab/labels/combined_windows.json");
     }
}

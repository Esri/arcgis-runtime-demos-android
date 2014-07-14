package com.esri.arcgis.sample;

import java.io.File;

import com.esri.core.geometry.SpatialReference;

public class Utils {

  public static final int INVALID_ID = -1;
  
  public static final SpatialReference WEB_MERCATOR = SpatialReference.create(SpatialReference.WKID_WGS84_WEB_MERCATOR_AUXILIARY_SPHERE);
  
  private static final String DATABASE_EXTENSION = ".geodatabase";
  
  private static final String TRANSPORTATION_EXTENTSION = ".tn";
  
  private static final String SCHEMA_EXTENSION = "_schema";
  
  private static final String LOCATOR_EXTENSION = ".loc";
  
  private static final String TPK_EXTENSION = ".tpk";
  
  public static boolean isLocalTiledLayer(File file) {    
    
    if (file == null || !file.exists() || file.isDirectory())
      return false;
    
    return file.getName().endsWith(TPK_EXTENSION);
    
  }
  
  /**
   * Returns true if a file is a geodatabase.
   * 
   * @param file The file to test.
   * @return true if the file is a geodatabase.
   */
  public static boolean isGeodatabase(File file) {
    
    if (file == null || !file.exists() || file.isDirectory())
      return false;
    
    return file.getName().endsWith(DATABASE_EXTENSION);
  }
  
  /**
   * Return true if a file is a Locator.
   * 
   * @param file The file to test.
   * @return true if the file is a Locator.
   */
  public static boolean isLocator(File file) {
    
    if (file == null || !file.exists() || file.isDirectory())
      return false;
    
    return file.getName().endsWith(LOCATOR_EXTENSION);    
  }
  
  /**
   * Extract the name of the transportation network from the given database path.
   * We are very careful here so as to not throw an exception.
   * 
   * @param databasePath The path to the database.
   * @return The name of the transportation network or null if it does not exist.
   */
  public static String findTransportationNetwork(String databasePath) {
    
    if (databasePath == null)
      return null;
    
    File database = new File(databasePath);
    if (!database.exists())
      return null;
    
    File transportationNetwork = new File(database.getAbsolutePath().replace(DATABASE_EXTENSION, TRANSPORTATION_EXTENTSION));
    if (!transportationNetwork.exists() || !transportationNetwork.isDirectory())
      return null;
    
    File[] files = transportationNetwork.listFiles();
    if (files == null || files.length == 0)
      return null;
    
    for (File file : files) {
      String fileName = file.getName();
      if (fileName.endsWith(SCHEMA_EXTENSION))
        return fileName.substring(0, fileName.length() - SCHEMA_EXTENSION.length());
    }
    
    return null;
  }
  
  /**
   * Does a case insensitive search for a String.
   * 
   * @param array The array to search through.
   * @param value The value to search for.
   * @return True if the value exists in the array, subject to a case insensitive comparison.
   */
  public static boolean containsCaseIgnore(String[] array, String value) {
    
    if (array == null || value == null)
      return false;
    
    for (String entry : array)
      if (value.equalsIgnoreCase(entry))
        return true;
    
    return false;    
  }
  
}

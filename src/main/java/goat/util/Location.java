package goat.util;

import goat.core.User;
import goat.core.Users;

import java.net.MalformedURLException;
import java.net.URL;

import goat.core.Users;
import goat.core.User;

public class Location {
  private double longitude;
  private double latitude;
  private boolean valid=false;
  private String errorString;
  
  public Location(String locStr) {
      if(locStr.contains("&ll")) {
          URL url;
          try {
              url = new URL(locStr);

              String query = url.getQuery();
              String[] args = query.split("&");
              for (int i=0; i < args.length; i++) {
                  String[] pair = args[i].split("=");
                  if (pair[0].equals("ll")) {
                      String[] coords = pair[1].split(",");
                      latitude = Double.parseDouble(coords[0]);
                      longitude = Double.parseDouble(coords[1]);
                      valid=true;
                  }
              }
          } catch (MalformedURLException me) {
              System.err.println("bad maps URL passed to getPosition():\n\t'" + locStr + "'");
              errorString="Bad maps url";
          }
      } else {
          Users users = new Users();
          if(users.hasUser(locStr)) {
              User user = users.getUser(locStr);
              latitude = user.getLatitude();
              longitude = user.getLongitude();
              valid=true;
          } else {
              errorString="User not found";
          }
      }
  }
  
  public Location(double lat, double lon) {
      this.latitude=lat;
      this.longitude=lon;
  }
  
  public boolean isValid() {
      return valid;
  }
  
  public double getLongitude() {
      return longitude;
  }
  
  public double getLatitude() {
      return latitude;
  }
  
  public double[] getPosPair() {
      return new double[]{latitude,longitude};
  }
  
  public String error() {
      return errorString;
  }
  
}

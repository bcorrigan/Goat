package goat.util;

import goat.core.User;
import goat.core.Users;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.Map;

import goat.core.Users;
import goat.core.User;

import static goat.util.StringUtil.parseUrlQueryString;;

public class Location {
  private double longitude;
  private double latitude;
  private boolean valid=false;
  private String errorString;
  
  public Location(String locStr) {
      //lets nail this finally
      if(locStr.contains("http")) {
          try {
              URL url = new URL(locStr);
              Map<String,String[]> reqMap = parseUrlQueryString(url.getQuery());
              String posStr=null;
              if(reqMap.containsKey("ll"))
                  posStr=reqMap.get("ll")[0];
              else if(reqMap.containsKey("sll"))
                  posStr=reqMap.get("sll")[0];
              else {
                  valid=false;
                  errorString="That url has no ll or sll";
              }
              
              if(posStr!=null) {
                  String[] coords = posStr.split(",");
                  latitude = Double.parseDouble(coords[0]);
                  longitude = Double.parseDouble(coords[1]);
                  valid=true;
              }
          } catch(MalformedURLException mfe) {
              valid=false;
              errorString="That's not a proper url.";
              return;
          }
      }  else {
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

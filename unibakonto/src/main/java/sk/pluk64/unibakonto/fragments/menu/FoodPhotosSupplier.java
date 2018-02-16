package sk.pluk64.unibakonto.fragments.menu;

import java.util.List;

import sk.pluk64.unibakonto.Utils;
import sk.pluk64.unibakonto.http.Util;

interface FoodPhotosSupplier {
    List<FBPhoto> getPhotos() throws Utils.FBAuthenticationException, Util.ConnectionFailedException;
}

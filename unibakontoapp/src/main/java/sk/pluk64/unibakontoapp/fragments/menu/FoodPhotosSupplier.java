package sk.pluk64.unibakontoapp.fragments.menu;

import java.util.List;

import sk.pluk64.unibakonto.Util;
import sk.pluk64.unibakontoapp.Utils;

interface FoodPhotosSupplier {
    List<FBPhoto> getPhotos() throws Utils.FBAuthenticationException, Util.ConnectionFailedException;
}

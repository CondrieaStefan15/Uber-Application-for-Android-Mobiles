package com.example.uber;

import java.util.regex.Pattern;

public class PayPalConfig {
    public static final String PAYPAL_CLIENT_ID = "AY0etGQTA_KVTHLcfic9ZIf9wJidRnKd2FWLa1vZZjZlzPoPof3pvlBk6WnUnnQknLM4hl-v37XvYnu7";
    public static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^" +
                    "(?=.*[0-9])" +         //at least 1 digit
                    //"(?=.*[a-z])" +         //at least 1 lower case letter
                    //"(?=.*[A-Z])" +         //at least 1 upper case letter
                    //"(?=.*[a-zA-Z])" +      //any letter
                    //"(?=.*[@#$%^&+=])" +    //at least 1 special character
                    "(?=\\S+$)" +           //no white spaces
                    ".{6,}" +               //at least 4 characters
                    "$");                   //end of string
}

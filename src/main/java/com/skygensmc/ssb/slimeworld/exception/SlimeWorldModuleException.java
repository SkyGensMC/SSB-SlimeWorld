package com.skygensmc.ssb.slimeworld.exception;

public class SlimeWorldModuleException extends Exception {

    public SlimeWorldModuleException(String error) {
        super(error);
    }

    public SlimeWorldModuleException(String error, Exception ex) {
        super(error, ex);
    }

}

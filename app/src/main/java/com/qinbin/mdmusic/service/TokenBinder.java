package com.qinbin.mdmusic.service;

import android.media.session.MediaSession;
import android.os.Binder;

/**
 * Created by Teacher on 2016/6/24.
 */
public class TokenBinder extends Binder implements  IToken{

    IToken inner;

    public TokenBinder(IToken inner) {
        this.inner = inner;
    }

    public MediaSession.Token getToken(){
        return inner.getToken();
    };
}

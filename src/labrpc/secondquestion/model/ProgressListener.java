/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package labrpc.secondquestion.model;

/**
 *
 * @author dmitry
 */
public interface ProgressListener {

    public void notifyProgress(Object obj, int amount);

    public void then(Object obj);

    public void onStart(Object obj);

    public void dmaSend(Object obj, long size);

    public void clear();
}

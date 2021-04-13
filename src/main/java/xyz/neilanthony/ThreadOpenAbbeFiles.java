

package xyz.neilanthony;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;


public class ThreadOpenAbbeFiles {
    
    
    // create array of FileInOAClass instances to match added filepaths

    // function to add strings to the queue
    
    // function to get metadata from file and store in persistent storage
    
    
    // need a queue that's global that can be added to
    
//    public void addToQueue(ArrayList<String> passedFilePaths) throws InterruptedException 
//    {
//        int numberPassed = passedFilePaths.size();
//        if (numberPassed > 0) {
//            
//            ArrayBlockingQueue<Integer> abQueue = new ArrayBlockingQueue<>(numberPassed);
//            
//            new Thread(() -> {
//                passedFilePaths.forEach((fp) -> abQueue);
//                {
//                    while (true) 
//                    {
//                        abQueue.put(++i);
//                        System.out.println("Added : " + i);
//
//                        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
//                    }
//
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//
//            }).start();
//            
//        }
//        
//        
// 
//
// 
//        //Consumer thread
//        new Thread(() -> 
//        {
//            try
//            {
//                while (true) 
//                {
//                    Integer poll = abQueue.take();
//                    System.out.println("Polled : " + poll);
//                     
//                    Thread.sleep(TimeUnit.SECONDS.toMillis(2));
//                }
// 
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
// 
//        }).start();
//    }
}



 


    
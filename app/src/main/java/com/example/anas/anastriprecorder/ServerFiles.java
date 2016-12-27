
package com.example.anas.anastriprecorder;

import android.util.SparseArray;

/*This class is used for enumeration of the php files on the server side */
class  ServerFiles {

    enum PhpFile{ file1 ,file2, file3 , file4, file5 }

    private final static SparseArray<String> serverFilesMap;
    static {
        serverFilesMap = new SparseArray<>();
        serverFilesMap.put(1, "FetchUserData.php");
        serverFilesMap.put(2, "FetchUserByEmail.php");
        serverFilesMap.put(3, "RegisterOrUpdateUser.php");
        serverFilesMap.put(4, "MCrypt.php");
        serverFilesMap.put(5, "AddTrip.php");

    }


    static String getFile(PhpFile phpFile){
        switch (phpFile){
            case file1 :
                return serverFilesMap.get(1);
            case file2 :
                return serverFilesMap.get(2);
            case file3 :
                return serverFilesMap.get(3);
            case file4 :
                return serverFilesMap.get(4);
            case file5 :
                return serverFilesMap.get(5);
            default    :
                return "Invalid file option";
        }
    }
}

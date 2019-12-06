package com.heng.ftp;

import com.heng.ftp.bean.SFTPFile;
import com.heng.ftp.client.SFTPClient;
import com.jcraft.jsch.SftpException;

import java.util.List;
import java.util.Vector;

    public class Test {

    public static void main(String[] args) throws SftpException {
        SFTPClient client = new SFTPClient();
        client.login();
        List<SFTPFile> list = client.ls("/root");
        for (SFTPFile file : list){
            System.out.println(file);
        }
//        String info = "-rwxr-xr-x    2 root     root         4096 Feb  8  2019 .vim";
//        String[] s = info.split("\\s+");
//
//        System.out.println(s.length);
    }
}
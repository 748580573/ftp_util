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
        List<SFTPFile> list = client.ls("/home/miner/install");
        list.stream().forEach(line -> System.out.println(line));
//        client.downLoad("/home/miner/install", "test.txt", "e:/test.txt");
        client.logout();

    }
}

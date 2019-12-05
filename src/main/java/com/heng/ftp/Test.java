package com.heng.ftp;

import com.heng.ftp.client.SFTPClient;
import com.jcraft.jsch.SftpException;

import java.util.Vector;

    public class Test {

    public static void main(String[] args) throws SftpException {
        SFTPClient client = new SFTPClient();
        client.login();
        Vector<?> list = client.ls("/root");
        list.stream().forEach(e -> System.out.println(e));
    }
}

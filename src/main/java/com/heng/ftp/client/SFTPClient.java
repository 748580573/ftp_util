package com.heng.ftp.client;

import com.heng.ftp.bean.SFTPFile;
import com.heng.ftp.util.PropertyUtil;
import com.jcraft.jsch.*;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

public class SFTPClient {

    Logger logger = LoggerFactory.getLogger(SFTPClient.class);

    private ChannelSftp sftp;

    private Session session;

    /**
     * SFTP 登录用户名
     */
    private String username;

    /**
     * SFTP 登录密码
     */
    private String password;

    /**
     * 私钥
     */
    private String privateKey;

    private String host;

    /**
     * SFTP端口
     */
    private int port;

    private PropertyUtil propertyUtil = PropertyUtil.getPropertyUtil("config.properties");

    public SFTPClient(){
        this.username = propertyUtil.getProperty("username");
        this.password = propertyUtil.getProperty("password");
        this.host = propertyUtil.getProperty("host");
        String test = propertyUtil.getProperty("port");
        this.port = Integer.valueOf(test.trim());
    }

    public void login(){
        try {
            JSch jSch = new JSch();
            if (privateKey != null){
                jSch.addIdentity(privateKey);
            }

            session = jSch.getSession(username,host,port);
            if (password != null){
                session.setPassword(password);
            }
            Properties config = new Properties();
            config.put("StrictHostKeyChecking","no");
            session.setConfig(config);
            session.connect();
            Channel channel = session.openChannel("sftp");
            channel.connect();
            sftp = (ChannelSftp) channel;
        } catch (JSchException e) {
            e.printStackTrace();
        }
    }
    
    
    public void logout(){
        if (sftp != null){
            if (sftp.isConnected()){
                sftp.disconnect();
            }
        }
    }

    /**
     * 上传文件
     * @param directory
     * @param sftpFileName
     * @param inputStream
     * @return
     */
    public boolean upload(String directory, String sftpFileName, InputStream inputStream){
        try {
            if (directory != null && !"".equalsIgnoreCase(directory)){
                sftp.cd(directory);
            }
            sftp.put(inputStream,sftpFileName);
        } catch (SftpException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     *
     * 下载文件
     * @param directory
     * @param downloadFile
     * @param saveFile
     */
    public void downLoad(String directory,String downloadFile,String saveFile){
        logger.info("download--directory:{},file{}",directory,downloadFile);
        File file = null;

        try {
            if (directory != null && !"".equals(directory)){
                sftp.cd(directory);
            }
            file = new File(saveFile);
            sftp.get(downloadFile,new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();

        } catch (SftpException e) {
            e.printStackTrace();
            if (file != null){
                file.delete();
            }
        }
    }

    /**
     * 下载文件
     * @param directory
     * @param downLoadFile
     * @return
     * @throws SftpException
     * @throws IOException
     */

    public byte[] download(String directory,String downLoadFile) throws SftpException, IOException {
        cd(directory);
        InputStream is = get(downLoadFile);

        byte[] fileData = IOUtils.toByteArray(is);

        return fileData;
    }


    /**
     * 切换目录
     * @param directory
     * @throws SftpException
     */
    private void cd(String directory) throws SftpException {
        if (directory != null && !"".equals(directory)){
            sftp.cd(directory);
        }
    }

    /**
     * 删除目录
     * @param downLoadFile
     * @return
     * @throws SftpException
     */
    private InputStream get(String downLoadFile) throws SftpException {
        return sftp.get(downLoadFile);
    }

    /**
     * 上传文件
     * @param sftpFileName
     * @param inputStream
     * @throws SftpException
     */
    private void put(String sftpFileName, InputStream inputStream) throws SftpException {
        sftp.put(inputStream,sftpFileName);
    }

    /**
     * 列出目录下的文件
     * @param directory
     * @return
     * @throws SftpException
     */
    public List<SFTPFile> ls(String directory) throws SftpException {
        Vector<?> files = sftp.ls(directory);
        List<SFTPFile> list = new ArrayList<>();
        SFTPFile file = new SFTPFile();
        files.stream().forEach(line -> {
            String[] s = ((Object) line).toString().split("\\s+");
            file.setDate(s[7]+" " + s[5] + " " + s[6]);
            file.setDirectory(s[0].startsWith("d"));
            file.setFileName(s[8]);
            file.setFileSize(s[4]);
            file.setUser(s[2]);
            file.setGroup(s[3]);
            file.setPermission(s[0].substring(1));
            file.setInfo(((Object) line).toString());
            list.add(file);
        });
        return list.size() < 0 ? null : list;
    }

    /**
     * 删除文件
     * @param directory
     * @param deleteFile
     * @throws SftpException
     */
    public void delete(String directory, String deleteFile) throws SftpException {
        cd(directory);
        sftp.rm(deleteFile);
    }

}

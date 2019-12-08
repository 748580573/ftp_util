package com.heng.ftp.client;

import com.heng.ftp.bean.SFTPFile;
import com.heng.ftp.util.PropertyUtil;
import com.jcraft.jsch.*;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

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
     * 文件分割符
     */
    private String separator = "/";

    /**
     * SFTP端口
     */
    private int port;

    private volatile String currentRemoteDir;

    private volatile String currentLocalDir;

    private String version;

    private PropertyUtil propertyUtil = PropertyUtil.getPropertyUtil("config.properties");

    /**
     * 下载文件的地址
     */
    private String downLocalLocation;

    private String downRemotelocation;

    /**
     * 远程下载文件地址是否为目录
     */
    private boolean downRemotelocationIsDir;

    public SFTPClient(){
        this.username = propertyUtil.getProperty("username");
        this.password = propertyUtil.getProperty("password");
        this.host = propertyUtil.getProperty("host");
        String test = propertyUtil.getProperty("port");
        this.port = Integer.valueOf(test.trim());
        this.downLocalLocation = propertyUtil.getProperty("downLoacation");
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
            this.currentRemoteDir = sftp.pwd();
            this.currentLocalDir = sftp.lpwd();
            this.version = sftp.version();
            if (null == downLocalLocation || downLocalLocation.length() == 0){
                downLocalLocation = sftp.lpwd();
            }
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (SftpException e) {
            e.printStackTrace();
        }
    }
    
    
    public void logout(){
        if (sftp != null){
            if (sftp.isConnected()){
                sftp.disconnect();
                sftp.exit();
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
     * 下载文件
     * @param remotePath
     */

    //TODO 将单线程下载改为多线程下载

    public void downLoad(String remotePath){
        logger.info("download--directory:{},file{}",remotePath);
        remotePath = remotePath.replace("\\", separator);
        this.downRemotelocation = remotePath;
        try {
            if (isDirectory(remotePath)){
                downRemotelocationIsDir = true;
                cd(remotePath);
                List<SFTPFile> list = ls(remotePath);
                Queue<SFTPFile> queue = new LinkedList<>();
                for (SFTPFile file : list){
                    queue.add(file);
                }

                while (!queue.isEmpty()){
                    SFTPFile sftpFile = queue.poll();
                    if (isDirectory(sftpFile.getFilePath())){
                        List<SFTPFile> tmp = ls(sftpFile.getFilePath());
                        for (SFTPFile file : tmp){
                            queue.add(file);
                        }
                    }else {
                        downLoadFile(sftpFile.getFilePath());
                    }
                }
            }else {
                downLoadFile(remotePath);
            }
        } catch (SftpException e) {
            e.printStackTrace();
        }
    }

    /**
     * 下载文件
     * @param remoteFilePath
     */
    private void downLoadFile(String remoteFilePath){
        String filename = getRemoteFileName(remoteFilePath);
        File file = new File(downLocalLocation + separator + filename);
        FileOutputStream out = null;
        try {
            //创建父目录
            if (!file.getParentFile().exists()){
                file.getParentFile().mkdir();
            }
//            if (!file.exists()){
//                file.createNewFile();
//            }

            out = new FileOutputStream(file);
            sftp.get(remoteFilePath,out);
        } catch (FileNotFoundException | SftpException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null){
                try {
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
     * 文件路径
     * @param filePath
     */
    private boolean isDirectory(String filePath) {
        try {
            List<SFTPFile> files = ls(filePath);
            if (files != null && files.size() == 1 && !files.get(0).isDirectory()){
                return false;
            }
        } catch (SftpException e) {
            e.printStackTrace();
        }

        return true;
    }


    /**
     * 切换目录
     * @param directory
     * @throws SftpException
     */
    public void cd(String directory) throws SftpException {
        if (directory != null && !"".equals(directory)){
            sftp.cd(directory);
            this.currentRemoteDir = directory;
        }
    }

    /**
     * 下载文件
     * @param downLoadFile
     * @return
     * @throws SftpException
     */
    public InputStream get(String downLoadFile) throws SftpException {
        return sftp.get(downLoadFile);
    }

    /**
     * 上传文件
     * @param sftpFileName
     * @param inputStream
     * @throws SftpException
     */
    public void put(String sftpFileName, InputStream inputStream) throws SftpException {
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
        files.stream().forEach(line -> {
            String[] s = ((Object) line).toString().split("\\s+");
            String s8 = s[8];
            if(!".".equals(s[8]) && !"..".equals(s[8])){
                SFTPFile file = new SFTPFile();
                file.setDate(s[7]+" " + s[5] + " " + s[6]);
                file.setDirectory(s[0].startsWith("d"));
                file.setFileName(s[8]);
                file.setFileSize(s[4]);
                file.setUser(s[2]);
                file.setGroup(s[3]);
                file.setPermission(s[0].substring(1));
                file.setInfo(((Object) line).toString());
                file.setFilePath(directory + separator + s[8]);
                list.add(file);
            }
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

    public void test(String directory) throws SftpException {
        this.ls(directory).stream().forEach(e -> System.out.println(e));
    }

    private String getRemoteFileName(String remoteFilePath){
        if (downRemotelocationIsDir){
            return remoteFilePath.substring(downRemotelocation.length());
        }
        return remoteFilePath.substring(remoteFilePath.lastIndexOf(separator) + 1);
    }
}

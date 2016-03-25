/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.rtpcam;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.Base64Encoder;


public class RTSPClient 
{
    static final Logger log = LoggerFactory.getLogger(RTSPClient.class);
    
    private final static String REQ_DESCRIBE = "DESCRIBE";
    private final static String REQ_SETUP = "SETUP";
    private final static String REQ_PLAY = "PLAY";
    //private final static String REQ_PAUSE = "PAUSE";
    private final static String REQ_GET_PARAMETER = "GET_PARAMETER";
    private final static String REQ_TEARDOWN = "TEARDOWN";   
    final static String CRLF = "\r\n";
    final static int INIT = 0;
    final static int READY = 1;
    final static int PLAYING = 2;
    
    int state;
    String videoUrl;
    String userName;
    String passwd;
    Socket rtspSocket;
    BufferedReader rtspResponseReader;
    BufferedWriter rtspRequestWriter;
    int rtspSeqNb = 0;          // RTSP sequence number within the session
    String rtspSessionID = "0"; // ID of the RTSP session (given by the RTSP Server)
    int rtpRcvPort;             // port where the client will receive the RTP packets
    
    // info obtained from RTSP server
    int remoteRtpPort;
    int remoteRtcpPort;
    String controlParam;
    String codecString;
    String paramSets;
    
    
    public RTSPClient(String serverHost, int serverPort, String videoPath, String login, String passwd, int rtpRcvPort) throws IOException
    {
        this.videoUrl = "rtsp://" + serverHost + ":" + serverPort + ((videoPath != null) ? videoPath : "");
        this.userName = login;
        this.passwd = passwd;
        
        InetAddress rtspServerIP = InetAddress.getByName(serverHost);
        this.rtspSocket = new Socket(rtspServerIP, serverPort);
        rtspSocket.setSoTimeout(5000);
        
        this.rtspResponseReader = new BufferedReader(new InputStreamReader(rtspSocket.getInputStream()));
        this.rtspRequestWriter = new BufferedWriter(new OutputStreamWriter(rtspSocket.getOutputStream()));

        this.rtpRcvPort = rtpRcvPort;
        this.state = INIT;
    }

    
    public void sendDescribe() throws IOException
    {
        sendRequest(REQ_DESCRIBE);
        parseResponse(REQ_DESCRIBE);
    }
    
    
    public void sendSetup() throws IOException
    {
        sendRequest(REQ_SETUP);
        parseResponse(REQ_SETUP);
    }
    
    
    public void sendPlay() throws IOException
    {
        sendRequest(REQ_PLAY);
        parseResponse(REQ_PLAY);
    }
    
    
    public void sendGetParameter() throws IOException
    {
        sendRequest(REQ_GET_PARAMETER);
        parseResponse(REQ_GET_PARAMETER);
    }
    
    
    public void teardown() throws IOException
    {
        try
        {
            sendRequest(REQ_TEARDOWN);
            parseResponse(REQ_TEARDOWN);
        }
        finally
        {
            try { rtspSocket.close(); }
            catch (IOException e) { }
        }        
    }
    
    
    private void sendRequest(String request_type) throws IOException
    {
        log.trace("Sending " + request_type + " Request");
        rtspSeqNb++;
        
        // write the request line:
        rtspRequestWriter.write(request_type + " ");
        if (request_type == REQ_SETUP)
        {
            if (controlParam.startsWith("rtsp://"))
                rtspRequestWriter.write(controlParam);
            else
                rtspRequestWriter.write(videoUrl + "/" + controlParam);            
        }
        else
        {
            rtspRequestWriter.write(videoUrl);
        }
        rtspRequestWriter.write(" RTSP/1.0" + CRLF);

        // write the CSeq line: 
        rtspRequestWriter.write("CSeq: " + rtspSeqNb + CRLF);
        addAuth();
        
        // depending on request type
        if (request_type == REQ_SETUP) {
            int rtcpPort = rtpRcvPort+1;
            rtspRequestWriter.write("Transport: RTP/AVP;unicast;client_port=" + rtpRcvPort + "-" + rtcpPort + CRLF);
        }
        else if (request_type == REQ_DESCRIBE) {
            rtspRequestWriter.write("Accept: application/sdp" + CRLF);
        }
        else {
            // otherwise, write the Session ID
            rtspRequestWriter.write("Session: " + rtspSessionID + CRLF);
        }
        
        // end header and flush
        rtspRequestWriter.write(CRLF);
        rtspRequestWriter.flush();
    }
    
    
    private void addAuth() throws IOException
    {
        if (userName != null && passwd != null)
        {
            String creds = userName + ":" + passwd;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Base64Encoder encoder = new Base64Encoder(baos);
            encoder.write(creds.getBytes());
            encoder.close();
            rtspRequestWriter.write("Authorization: Basic " + new String(baos.toByteArray()) + CRLF);
        }
    }
    
    
    private void parseResponse(String reqType) throws IOException
    {
        String line = rtspResponseReader.readLine();
        
        // read response code
        int respCode = Integer.parseInt(line.split(" ")[1]);
        if (respCode != 200)
            throw new IOException("RTSP Server Error: " + respCode);
        
        // parse response according to request type
        if (reqType == REQ_DESCRIBE)
            parseDescribeResp();        
        else if (reqType == REQ_SETUP)
            parseSetupResp();        
        else
            printResponse();
    }
    
    
    private void parseDescribeResp() throws IOException
    {
        int contentLength = 0;
        BufferedReader reader = rtspResponseReader;
        
        // read header, then content
        String line;
        while ((line = reader.readLine()) != null)
        {
            // detect end of header
            if (line.length() == 0)
            {
                char[] cbuf = new char[contentLength];
                rtspResponseReader.read(cbuf);
                reader = new BufferedReader(new CharArrayReader(cbuf));
                continue;
            }
            
            log.trace("> {}", line);
            
            try
            {
                String[] tokens = line.split(":");
                String key = tokens[0];
                
                // header stuff
                if (key.equalsIgnoreCase("Content-Length"))
                {
                    contentLength = Integer.parseInt(tokens[1].trim());
                }
                
                // SDP content stuff
                else if (line.startsWith("a=control:"))
                {
                    controlParam = line.substring(line.indexOf(':')+1);
                    log.debug("> Control Param: {}", controlParam);
                }
                else if (line.startsWith("a=rtpmap"))
                {
                    codecString = line.substring(line.indexOf(':')+1);                    
                }
                else if (line.startsWith("a=fmtp:96"))
                {
                    for (String token: line.split("; "))
                    {
                        if (token.startsWith("sprop-parameter-sets"))
                        {
                            paramSets = token.substring(token.indexOf('=')+1).trim();
                            log.debug("> Parameter Sets: {}", paramSets);
                            break;
                        }
                    }
                }                       
            }
            catch (Exception e)
            {
                throw new IOException("Invalid DESCRIBE response", e);
            }
        }
    }
    
    
    private void parseSetupResp() throws IOException
    {
        String line;
        while ((line = rtspResponseReader.readLine()) != null)
        {
            if (line.length() == 0)
                break;
            else
                log.trace("> {}", line);
            
            try
            {
                if (line.startsWith("Session:"))
                {
                    rtspSessionID = line.split(" |;")[1];
                    log.debug("> Session ID: {}", rtspSessionID);
                }
                
                else if (line.startsWith("Transport:"))
                {
                    int serverPortIdx = line.indexOf("server_port=") + 12;
                    String serverPortString = line.substring(serverPortIdx, line.indexOf(';', serverPortIdx));
                    String[] ports = serverPortString.split("-");
                    remoteRtpPort = Integer.parseInt(ports[0]);
                    remoteRtcpPort = Integer.parseInt(ports[1]);
                    log.debug("> Server ports: RTP {}, RTCP {}", remoteRtpPort, remoteRtcpPort);
                }
            }
            catch (Exception e)
            {
                throw new IOException("Invalid SETUP response", e);
            }
        }
    }
    
    
    private void printResponse() throws IOException
    {
        String line;
        while ((line = rtspResponseReader.readLine()) != null)
        {
            if (line.length() == 0)
                break;
            else
                log.trace("> {}", line);
        }
    }
    
    
    public int getRemoteRtpPort()
    {
        return remoteRtpPort;
    }
    
    
    public int getRemoteRtcpPort()
    {
        return remoteRtcpPort;
    }
    
    
    public String getCodecString()
    {
        return codecString;
    }
    
    
    public String getParameterSets()
    {
        return paramSets;
    }
}
package com.Devoir2.HDLC;

import java.io.UnsupportedEncodingException;

public class Frame {
	// Partial Frames can exist as long as it's not a valid one.
	private boolean valid;
	private String message,encoded;
	
	public Frame(String msg,boolean toBeEncoded){
		valid=true;
		if(toBeEncoded) {
			message=msg;
			encode();
		}else{
			encoded=msg;
			decode();
		}
	}
	
	public String getMessage() {
		return message;
	}
	
	public String getFrame() {
		return encoded;
	}
	
	public Boolean isValid() {
		return valid;
	}
    
    private void encode() {
    	// The actual message (Type+Num+(Data)?)is converted to bytes then the checksum is added
    	String tmp=checksum(characterToByte());
    	
    	// Bit stuffing
    	int cnt = 0;
    	for(int i=0;i<tmp.length();i++) {
    		if (tmp.charAt(i)=='0')
    			cnt=0;
    		else {
    			cnt++;
    			if(cnt==5) {
    				tmp=tmp.substring(0,i+1)+0+tmp.substring(i+1);
    			}
    		}
    	}
    	
    	// Flags and final assignation is done here
    	encoded="01111110"+tmp+"01111110";
    }
    
    private void decode() {
    	// Removing flags and verification.
    	int cnt = encoded.indexOf("01111110")+8;
    	if(cnt==7||encoded.substring(cnt).indexOf("01111110")==-1) {
    		valid=false;
    		message="";
    		return;
    	}
    	String tmp = encoded.substring(cnt,encoded.substring(cnt).indexOf("01111110")+cnt);
    	
    	// Removing bit stuffing
    	cnt = 0;
    	for(int i=0;i<tmp.length();i++) {
    		if (tmp.charAt(i)=='0')
    			cnt=0;
    		else {
    			cnt++;
    			if(cnt==5) {
    				if(i==tmp.length()-1||tmp.charAt(i+1)!='0') {
    					valid=false;
    					return;
    				}
    				tmp=tmp.substring(0,i+1)+tmp.substring(i+2);
    				cnt=0;
    			}
    		}
    	}
    	
    	// Checksum verification
    	if(tmp.length()<16) {
    		valid=false;
    		return;
    	}
    	String resp=tmp.substring(0, tmp.length()-16);
    	if(!tmp.equals(checksum(resp))) {
    		valid=false;
    		return;
    	}
    	message=resp;
    	
    	// Message changed from bytes to characters
    	toCharacter();
    }
    
    private String checksum(String msg) {
    	String tmp=msg+"0000000000000000";
    	char tmp2,tmp3,tmp4;
    	for(int i = 0; i<tmp.length()-16;i++) {
    		// Le code gÃ©nÃ©rateur a Ã©tÃ© hardcodÃ© ici. 10001000000100001
    		if(tmp.charAt(i)=='1') {
    			tmp2=tmp.charAt(i+4)=='0'?'1':'0';
    			tmp3=tmp.charAt(i+11)=='0'?'1':'0';
    			tmp4=tmp.charAt(i+16)=='0'?'1':'0';
    			tmp=tmp.substring(0, i)+'0'+tmp.substring(i+1,i+4)+tmp2+tmp.substring(i+5,i+11)+tmp3+tmp.substring(i+12, i+16)+tmp4+tmp.substring(i+17);
    		}
    	}
    	return msg+tmp.substring(tmp.length()-16);
    }
    
    private String characterToByte() {
    	String resp="";
    	byte[] bMessage=message.getBytes();
    	for(int i=0;i<bMessage.length;i++) {
    		int tmp=bMessage[i];
    		if (tmp<0) {
    			tmp+=128;
    			resp+='1';
    		}else
    			resp+='0';
    		for(int j = 0;j<7;j++) {
    			if(tmp-Math.pow(2,(6-j))>=0) {
    				resp+='1';
    				tmp-=Math.pow(2,(6-j));
    			}else
        			resp+='0';
    		}
    	}
		return resp;
    }
    
    private void toCharacter() {
    	if(message.length()<16||Math.floor((message.length()/8)*8)!=message.length()) {
    		valid=false;
    		return;
    	}
    	byte[] resp=new byte[message.length()/8];
    	for(int i=0;i<resp.length;i++) {
    		String tmp=message.substring(i*8,i*8+8);
    		if (tmp.charAt(0)=='1') {
    			resp[i]=-128;
    		}else
    			resp[i]=0;
    		for(int j = 0;j<7;j++) {
    			if(tmp.charAt(j+1)=='1') {
    				resp[i]+=Math.pow(2,(6-j));
    			}
    		}
    	}
		try {
			message= new String(resp,"UTF-8");
	    	boolean j=false;
	    	for(int i=0;i<8;i++) {
	    		j=j||(""+i).equals(message.substring(1,2));
	    	}
	    	valid=valid&&j;
		} catch (UnsupportedEncodingException e) {
			valid=false;
		}
    }
}

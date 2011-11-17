package de.viktorreiser.toolbox.util;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import jcifs.smb.NtStatus;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/**
 * Static samba helper utilities (<b>Beta</b>).<br>
 * <br>
 * This class needs <a href="http://jcifs.samba.org/">JCIFS samba</a> library.<br>
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public final class SmbUtils {
	
	// PRIVATE ====================================================================================
	
	/** Pattern which matches an IP. */
	private static Pattern IP_PATTERN = Pattern.compile("^[0-9]{1,3}(\\.[0-9]{1,3}){3}$");
	
	// PUBLIC =====================================================================================
	
	/**
	 * Was access denied for samba request?<br>
	 * <br>
	 * That means that given login information might be wrong.
	 * 
	 * @param e
	 *            exception to check
	 * 
	 * @return {@code true} if exception has access denied status
	 */
	public static boolean isAccessDenied(SmbException e) {
		return e.getNtStatus() == NtStatus.NT_STATUS_ACCESS_DENIED
				|| e.getNtStatus() == NtStatus.NT_STATUS_WRONG_PASSWORD
				|| e.getNtStatus() == NtStatus.NT_STATUS_LOGON_FAILURE
				|| e.getNtStatus() == NtStatus.NT_STATUS_NO_SUCH_USER;
	}
	
	/**
	 * Is host offline?
	 * 
	 * @param e
	 *            exception to check
	 * 
	 * @return {@code true} if exception indicates an unreachable host
	 */
	public static boolean isHostUnreachable(SmbException e) {
		return e.getNtStatus() == NtStatus.NT_STATUS_UNSUCCESSFUL;
	}
	
	/**
	 * Get list with available hosts from samba network.<br>
	 * <br>
	 * This method is time critical because it will contact the network. You could do that once and
	 * cache result. Do that on another thread. Be aware of the fact a network request could fail
	 * and give an empty or incomplete list although host should be available.<br>
	 * <br>
	 * <b>Note</b>: Don't forget to request for Internet permission in manifest!
	 * 
	 * @param withIp
	 *            {@code true} when IP of host should be listed too (does it anyway when found host
	 *            is not a name but an IP)
	 * 
	 * @return list with all available (found) hosts
	 */
	public static String [] listAvailableHosts(boolean withIp) {
		List<String> hostNames = new ArrayList<String>();
		
		try {
			SmbFile [] workgroups = new SmbFile("smb://").listFiles();
			
			for (int i = 0; i < workgroups.length; i++) {
				try {
					SmbFile [] hosts = workgroups[i].listFiles();
					
					// check hosts in workgroup
					for (int j = 0; j < hosts.length; j++) {
						String name = hosts[j].getName();
						String nameWithoutSlash = name.substring(0, name.length() - 1);
						hostNames.add(nameWithoutSlash);
						
						if (withIp && !IP_PATTERN.matcher(nameWithoutSlash).matches()) {
							try {
								hostNames.add(InetAddress.getByName(nameWithoutSlash)
										.getHostAddress());
							} catch (UnknownHostException e) {
								// could not resolve IP - skip it
							}
						}
					}
				} catch (SmbException e) {
					// can't retrieve list of host from workgroup - skip it
				}
			}
		} catch (SmbException e) {
			// smb:// should be valid - just skip on error
		} catch (MalformedURLException e) {
			// should never happen - smb:// is valid
			throw new RuntimeException(e);
		}
		
		String [] hosts = hostNames.toArray(new String [0]);
		
		return hosts;
	}
	
	/**
	 * Get samba file with URL safe encoded string.
	 * 
	 * @param auth
	 *            {@link #encodeAuth(String, String)} or empty
	 * @param host
	 *            host as name or IP
	 * @param path
	 *            path on host
	 * 
	 * @return samba file
	 */
	public static SmbFile getFile(String auth, String host, String path) {
		StringBuilder realPath = new StringBuilder();
		String trimmedPath = path.trim();
		int trimmedPathLength = trimmedPath.length();
		
		if (trimmedPathLength != 0) {
			String [] folders = path.split("/+");
			
			// join folders and encode as URL
			for (int i = 0; i < folders.length; i++) {
				if (!folders[i].trim().equals("")) {
					realPath.append(URLEncoder.encode(folders[i]));
					realPath.append("/");
				}
			}
			
			// remove trailing slash if there was no before
			if (trimmedPath.charAt(trimmedPathLength - 1) != '/') {
				realPath.deleteCharAt(realPath.length() - 1);
			}
		}
		
		try {
			return new SmbFile("smb://" + auth + URLEncoder.encode(host) + "/" + realPath);
		} catch (MalformedURLException e) {
			// URL should be valid
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Get samba file with URL encoded path.
	 * 
	 * @param user
	 *            user or empty
	 * @param password
	 *            password or empty
	 * @param host
	 *            host as name or IP
	 * @param path
	 *            path on host
	 * 
	 * @return samba file
	 */
	public static SmbFile getFile(String user, String password, String host, String path) {
		return getFile(encodeAuth(user, password), host, path);
	}
	
	/**
	 * Encode user and password for URL.<br>
	 * <br>
	 * If user is empty you get an empty string.<br>
	 * If password is empty you get {@code user@}.
	 * 
	 * @param user
	 *            user
	 * @param password
	 *            password
	 * 
	 * @return URL encoded string with format {@code user:password@}
	 */
	public static String encodeAuth(String user, String password) {
		StringBuilder auth = new StringBuilder();
		
		if (user != null && !user.trim().equals("")) {
			auth.append(URLEncoder.encode(user));
			
			if (password != null && !password.trim().equals("")) {
				auth.append(":");
				auth.append(URLEncoder.encode(password));
			}
			
			auth.append("@");
		}
		
		return auth.toString();
	}
	
	// PRIVATE ====================================================================================
	
	/**
	 * No constructor for static class.
	 */
	private SmbUtils() {
		
	}
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.codehaus.swizzle.confluence;

import static java.lang.String.format;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import org.bsc.confluence.ConfluenceProxy;
import org.bsc.confluence.ConfluenceService;
import org.bsc.confluence.ExportFormat;
import org.bsc.ssl.SSLCertificateInfo;
import rx.functions.Action1;

/**
 *
 * @author bsorrentino
 */
public class XMLRPCConfluenceServiceImpl implements ConfluenceService {

    public final Confluence connection;
    public final Credentials credentials;

    /**
     * 
     * @param url
     * @param proxyInfo
     * @return
     * @throws MalformedURLException
     * @throws SwizzleException
     * @throws URISyntaxException 
     */
    public static XMLRPCConfluenceServiceImpl createInstanceDetectingVersion( String url, Credentials credentials, ConfluenceProxy proxyInfo, SSLCertificateInfo sslInfo ) throws Exception {
        if( url == null ) {
            throw new IllegalArgumentException("url argument is null!");
        }
        if( credentials == null ) {
            throw new IllegalArgumentException("credentials argument is null!");
        }
        if( sslInfo == null ) {
            throw new IllegalArgumentException("sslInfo argument is null!");
        }

        if (!sslInfo.isIgnore() && url.startsWith("https")) {
            
            HttpsURLConnection.setDefaultSSLSocketFactory( sslInfo.getSSLSocketFactory());

            HttpsURLConnection.setDefaultHostnameVerifier( sslInfo.getHostnameVerifier() );
        }
        
        final Confluence c = new Confluence(url, proxyInfo);
        
        c.login(credentials.username, credentials.password);
        
        final ServerInfo info = c.getServerInfo();
        
        return new XMLRPCConfluenceServiceImpl( (info.getMajorVersion() < 4) ? c : new Confluence2(c), credentials );
        
    }
    
    /**
     * 
     * @param confluence 
     */
    protected XMLRPCConfluenceServiceImpl(Confluence confluence, Credentials credentials ) {
        if( confluence==null ) {
            throw new IllegalArgumentException("confluence argument is null!");
        } 
        if( credentials==null ) {
            throw new IllegalArgumentException("credentials argument is null!");
        } 
        this.connection = confluence;
        this.credentials = credentials;
    }

    @Override
    public Credentials getCredentials() {
        return credentials;
    }
 
    
    /**
     * 
     * @param parentPageId
     * @param title
     * @return
     * @throws Exception 
     */
    @Override
    public Model.PageSummary findPageByTitle(String parentPageId, String title) throws Exception {
        if( parentPageId == null ) {
            throw new IllegalArgumentException("parentPageId argument is null!");
        }
        if( title == null ) {
            throw new IllegalArgumentException("title argument is null!");
        }
        
        final List<PageSummary> children = connection.getChildren(parentPageId);

        for (PageSummary pageSummary : children ) {

        	if( title.equals(pageSummary.getTitle())) {
        		return (Model.PageSummary) pageSummary;
        	}
        }

        return null;
    }

    private Page cast( Model.Page page ) {
        if( page == null ) {
            throw new IllegalArgumentException("page argument is null!");
        }
        if( !(page instanceof Page) ) {
            throw new IllegalArgumentException("page argument is not right type!");
        }
        return (Page)page;

    }
    private Attachment cast( Model.Attachment attachment ) {
        if( attachment == null ) {
            throw new IllegalArgumentException("attachment argument is null!");
        }
        if( !(attachment instanceof Attachment) ) {
            throw new IllegalArgumentException("page argument is not right type!");
        }
        return (Attachment)attachment;

    }

    @Override
    public boolean removePage(Model.Page parentPage, String title) throws Exception {
        if( null==parentPage ) {
            throw new IllegalArgumentException("parentPage is null");
        }

        final Model.PageSummary pageSummary = findPageByTitle( parentPage.getId(), title);

        if( pageSummary!=null ) {
            connection.removePage(pageSummary.getId());
            return true;
        }

        return false;
    }

    @Override
    public Model.Page getOrCreatePage(String spaceKey, String parentPageTitle, String title) throws Exception {

            final Page parentPage = connection.getPage(spaceKey, parentPageTitle);

            return getOrCreatePage(parentPage, title);
    }

    @Override
    public Model.Page getOrCreatePage(Model.Page parentPage, String title) throws Exception {

            final Model.PageSummary pageSummary = findPageByTitle(parentPage.getId(), title);

            Page result;

            if (null != pageSummary) {
                result = connection.getPage(pageSummary.getId());
            } else {
                result = new Page(Collections.emptyMap());
                result.setSpace(parentPage.getSpace());
                result.setParentId(parentPage.getId());
                result.setTitle(title);

            }

            return result;
        
    }

    @Override
	public Model.Attachment addAttchment(Model.Page page, Model.Attachment attachment, File source) throws Exception {
		final Page p = cast(page);

		if (p.getId() == null) {
			throw new IllegalStateException("PageId is null. Attachment cannot be added!");
		}

		final Attachment a = cast(attachment);
		InputStream is = null;
		BufferedInputStream fis = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			is = new FileInputStream(source);
			fis = new BufferedInputStream(is, 4096);

			byte[] readbuf = new byte[4096];

			int len;

			while ((len = fis.read(readbuf)) == readbuf.length) {
				baos.write(readbuf, 0, len);
			}
			if (len > 0)
				baos.write(readbuf, 0, len);

			a.setPageId(page.getId());
		} finally {
			if (is != null) {
				is.close();
			}
			if (fis != null) {
				fis.close();
			}
		}
		return connection.addAttachment(new Long(page.getId()), a, baos.toByteArray());
	}

    @Override
    public Model.Page storePage(Model.Page page) throws Exception {
        final Page p = cast(page);
        
        return (Model.Page) connection.storePage(p);
    }

   @Override
    public Model.Page storePage(Model.Page page, Storage content) throws Exception {
        if( content == null ) {
            throw new IllegalArgumentException("content argument is null!");
        }
        
        final Page p = cast(page);
        
        p.setContent(content.value);
        
        return (Model.Page) connection.storePage(p);
    }
    
    /**
     *
     * @param confluence
     */
    public boolean logout() {

        try {
            if (!connection.logout()) {
                //log.error("connection logout has failed!", null);
                return false;
            }
        } catch (Exception e) {
            //log.error("connection logout has failed due exception ", e);
            return false;
        }
    
        return true;

    }

    /**
     * 
     * @param label
     * @param id
     * @return
     * @throws Exception 
     */
    @Override
    public boolean addLabelByName(String label, long id) throws Exception {
        return connection.addLabelByName(label, id);
    }
    

    @Override
    public Model.Attachment createAttachment() {
        return new Attachment();
    }

    @Override
    public Model.Attachment getAttachment(String pageId, String name, String version) throws Exception {     
        return connection.getAttachment(pageId, name, version);
    }

    @Override
    public Model.Page getPage(String spaceKey, String pageTitle) throws Exception {
        return connection.getPage(spaceKey, pageTitle);
    }

    @Override
    public Model.Page getPage(String pageId) throws Exception {
        return connection.getPage(pageId);
    }

    @Override
    public String toString() {
        try {
            final ServerInfo si = connection.getServerInfo();
            
            return format("Confluence version [%d.%d.%d-%s] development version [%b]",  
                            si.getMajorVersion(),                                                                                        
                            si.getMinorVersion(), 
                            si.getPatchLevel(), 
                            si.getBuildId(), 
                            si.isDevelopmentBuild());
            
        } catch (Exception ex) {
            // TODO LOG
            return ex.getMessage();
        }
    }

    @Override
    public void call(Action1<ConfluenceService> task) throws Exception {
        
        try {
            task.call(this);
        }
        finally {
            logout();
        }
    }

    @Override
    public List<Model.PageSummary> getDescendents(String pageId) throws Exception {        
        return connection.getDescendents(pageId);
    }

    @Override
    public void removePage(String pageId) throws Exception {
        connection.removePage(pageId);
    }

    @Override
    public void exportPage( String url, 
                            String spaceKey, 
                            String pageTitle, 
                            ExportFormat exfmt, 
                            File outputFile) throws Exception 
    {
            final ConfluenceExportDecorator exporter = 
                new ConfluenceExportDecorator(  connection, 
                                                url, 
                                                credentials.username, 
                                                credentials.password);

            exporter.exportPage(spaceKey, 
                                pageTitle, 
                                exfmt, 
                                outputFile);

    }

}

# The API server
upstream evec {

}

# The current Python web workers.
# Use multiple ports!
upstream evec_web {

}

# The root server - this redirects to TLS, except for the API.
server {
	server_name eve-central.com www.eve-central.com;
	listen [::]:80 default_server ipv6only=on;
	listen 80 default_server;

	# Exceptions for legacy uses
	location ^~ /api/ {
	     	 proxy_pass http://evec;
	} 
	location ^~ /datainput.py {
		 proxy_pass http://evec;
	}	      
	location = /nginx_status {
		 stub_status on;
         	 access_log off;
		 allow 127.0.0.1;
		 deny all;
        }
	location ^~ /dumps {
	      autoindex on;
	      expires 1d;
	}

	# Default case: redirect to SSL
	location / {
		 return 301 https://eve-central.com$request_uri;
	}

	access_log #log
	error_log #log

}

# The main TLS server, which also serves the API for
# TLS reasons (no need for dual certs or a wildcard)
server {
	server_name eve-central.com www.eve-central.com;
	root # static_web

	listen [::]:443 ssl;
	index index.html index.htm;

	ssl_certificate # cert
	ssl_certificate_key # key
	ssl_session_timeout 30m;
        ssl_protocols TLSv1 TLSv1.1 TLSv1.2;
	      
	access_log #log
	error_log #log

	# Legacy IGB header needs to passed through
	proxy_pass_header Eve-Regionname;

	proxy_http_version 1.1;


	location ^~ /datainput.py {
	      proxy_pass http://evec;
	}	      

	location ^~ /api/ {
	      proxy_pass http://evec;
	} 

	location / {
	      rewrite ^/tradetool$ /tradetool/ permanent;
	      proxy_pass http://evec_web;
	}
	
	location ^~ /js/ {
	      expires 1d;
	}

	location ^~ /css/ {
	      expires 1d;
	}

	location ^~ /dumps {
	      autoindex on;
	      expires 1d;
	}

	location ^~ /images/ {
	      expires 1d;
	}

	# Long dead API - explicitly reject to avoid hitting the
	# Python endpoints
	location = /home/marketstat_xml.html {
		 return 410;
	}
}



server {
	server_name api.eve-central.com;
	index index.html index.htm;

	access_log #log
	error_log #log

	proxy_http_version 1.1;

	location ^~ /api {
	      proxy_pass http://evec;
	}
}

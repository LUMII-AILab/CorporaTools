# -*- cperl -*-

package IOBackend;
use Exporter;
use File::Temp 0.14 qw();
use IO::File;
use IO::Pipe;
use strict;
use URI;
use URI::file;
use URI::Escape;
use Carp;
use LWP::UserAgent;

use Cwd qw(getcwd);

use vars qw(@ISA $VERSION @EXPORT @EXPORT_OK
            %UNLINK_ON_CLOSE
	    $Debug
	    $kioclient $kioclient_opts
	    $ssh $ssh_opts
	    $curl $curl_opts
	    $gzip $gzip_opts
	    $zcat $zcat_opts
	    $reject_proto
	    $lwp_user_agent
	   );

{
  package IOBackend::UserAgent;
  use base qw(LWP::UserAgent);
}

#$Debug=0;
my %input_protocol_handler;

BEGIN {
  *_find_exe = eval {
      require File::Which;
      \&File::Which::which
  } || sub {};

  $VERSION = "0.2";
  @ISA=qw(Exporter);
  @EXPORT_OK = qw($kioclient $kioclient_opts
		  $ssh $ssh_opts
		  $curl $curl_opts
		  $gzip $gzip_opts
		  $zcat $zcat_opts
		  &set_encoding
		  &open_backend &close_backend
		  &get_protocol &quote_filename);

  $zcat	      ||= _find_exe('zcat');
  $gzip	      ||= _find_exe('gzip');
  $kioclient  ||= _find_exe('kioclient');
  $ssh	      ||= _find_exe('ssh');
  $curl	      ||= _find_exe('curl');
  $ssh_opts   ||= '-C';
  $reject_proto ||= '^(pop3?s?|imaps?)\$';
  $lwp_user_agent = IOBackend::UserAgent->new(keep_alive=>1);
  $lwp_user_agent->agent("TrEd_IOBackend/$VERSION");
};

sub register_input_protocol_handler {
  my ($proto,$handler)=@_;
  if (ref($handler) eq 'CODE' or ref($handler) eq 'ARRAY') {
    if (exists($input_protocol_handler{$proto})) {
      carp("IOBackend::register_input_protocol_handler: WARNING: redefining protocol handler for '$proto'");
    }
    $input_protocol_handler{$proto}=$handler;
  } else {
    croak("Wrong arguments. Usage: IOBackend::register_input_protocol_handler(protocol=>callback)");
  }
}

sub unregister_input_protocol_handler {
  my ($proto)=@_;
  return delete $input_protocol_handler{$proto};
}

sub get_input_protocol_handler {
  my ($proto)=@_;
  return $input_protocol_handler{$proto};
}

sub set_encoding {
  my ($fh,$encoding) = @_;
  no integer;
  if (defined($fh) and defined($encoding) and ($]>=5.008)) {
    eval {
      binmode($fh,":raw:perlio:encoding($encoding)");
    };
    warn $@ if $@;
  }
  return $fh;
}

# to avoid collision with Win32 drive-names, we only support protocols
# with at least two letters
sub get_protocol {
  my ($uri) = @_;
  if (ref($uri) and UNIVERSAL::isa($uri,'URI')) {
    return $uri->scheme || 'file';
  }
  if ($uri =~ m{^\s*([[:alnum:]][[:alnum:]]+):}) {
    return $1;
  } else {
    return 'file';
  }
}

sub quote_filename {
  my ($uri)=@_;
  $uri =~ s{\\}{\\\\}g;
  $uri =~ s{"}{\\"}g;
  return '"'.$uri.'"';
}

sub get_filename {
  my ($uri)=@_;
  $uri=make_URI($uri); # cast to URI or make a copy
  $uri->scheme('file') if !$uri->scheme;
  if ($uri->scheme eq 'file') {
    return $uri->file;
  }
}

sub make_abs_URI {
  my ($url)=@_;
  my $uri = make_URI($url);
  my $cwd = getcwd();
  $cwd = VMS::Filespec::unixpath($cwd) if $^O eq 'VMS';
  $cwd = URI::file->new($cwd);
  $cwd .= "/" unless substr($cwd, -1, 1) eq "/";
  return $uri->abs($cwd);
}

sub make_URI {
  my ($url)=@_;
  my $uri = URI->new($url);
  return $uri if UNIVERSAL::isa($url,'URI'); # return a copy if was URI already
  if (($uri eq $url or URI::Escape::uri_unescape($uri) eq $url)
	and $url =~ m(^\s*[[:alnum:]]+://)) { # looks like it is URL already
    return $uri;
  } else {
    return URI::file->new($url);
  }
}

sub make_relative_URI {
  my ($href,$base)=@_;
#  if (Fslib::_is_url($href)) {
  $href = URI->new(make_URI($href)) unless UNIVERSAL::isa($href,'URI');
  $base = make_URI($base);
  ###  $href = $href->abs($base)->rel($base);
  $href = $href->rel($base);
}

sub strip_protocol {
  my ($uri)=@_;
  $uri=make_URI($uri); # make a copy
  $uri->scheme('file') if !$uri->scheme;
  if ($uri->scheme eq 'file') {
    return $uri->file;
  }
  return $uri->opaque;
}

sub _is_gzip {
  ($_[0] =~/.gz~?$/) ? 1 : 0;
}

sub is_same_filename {
  my ($f1,$f2)=@_;
  return 1 if $f1 eq $f2;
  my $u1 = UNIVERSAL::isa($f1,'URI') ? $f1 : make_URI($f1);
  my $u2 = UNIVERSAL::isa($f2,'URI') ? $f2 : make_URI($f2);
  return 1 if $u1 eq $u2;
  return 1 if $u1->canonical eq $u2->canonical;
  if (!ref($f1) and !ref($f2) and $^O ne 'MSWin32' and -f $f1 and -f $f2) {
    return is_same_file($f1,$f2);
  }
  return 0;
}


sub is_same_file {
  my ($f1,$f2) = @_;
  return 1 if $f1 eq $f2;
  my ($d1,$i1)=stat($f1);
  my ($d2,$i2)=stat($f2);
  return ($d1==$d2 and $i1!=0 and $i1==$i2) ? 1 : 0;
}


sub open_pipe {
  my ($file,$rw,$pipe) = @_;
  my $fh;
  if (_is_gzip($file)) {
    if (-x $gzip && -x $zcat) {
      if ($rw eq 'w') {
	open $fh, "| $pipe | $gzip $gzip_opts > ".quote_filename($file) || undef $fh;
      } else {
	open $fh, "$zcat $zcat_opts < ".quote_filename($file)." | $pipe |" || undef $fh;
      }
    } else {
      warn "Need a functional gzip and zcat to open this file\n";
    }
  } else {
    if ($rw eq 'w') {
      open $fh, "| $pipe > ".quote_filename($file) || undef $fh;
    } else {
      open $fh, "$pipe < ".quote_filename($file)." |" || undef $fh;
    }
  }
  return $fh;
}

# open_file_zcat:
#
# Note: This function represents the original strategy used on POSIX
# systems. It turns out, however, that the calls to zcat/gzip cause
# serious penalty on btred when loading large amount of files and also
# cause the process' priority to lessen. It also turns out that we
# cannot use IO::Zlib filehandles directly with some backends, such as
# StorableBackend.
#
# I'm leaving the function here, but it is not used anymore.

sub open_file_zcat {
  my ($file,$rw) = @_;
  my $fh;
  if (_is_gzip($file)) {
   if (-x $gzip) {
      $fh = new IO::Pipe();
      if ($rw eq 'w') {
	$fh->writer("$gzip $gzip_opts > ".quote_filename($file)) || undef $fh;
      } else {
	$fh->reader("$zcat $zcat_opts < ".quote_filename($file)) || undef $fh;
      }
   }
   unless ($fh) {
     eval {
       require IO::Zlib;
       $fh = new IO::Zlib;
     } || return;
     $fh->open($file,$rw."b") || undef $fh;
   }
  } else {
    $fh = new IO::File();
    $fh->open($file,$rw) || undef $fh;
  }
  return $fh;
}

sub open_file {
  my ($file,$rw) = @_;
  my $fh;
  if (_is_gzip($file)) {
    eval {
      $fh = File::Temp->new(UNLINK => 1);
    };
    die if $@;
    return unless $fh;
    if ($rw eq 'w') {
      print "IOBackend: Storing ZIPTOFILE: $rw\n" if $Debug;
      ${*$fh}{'ZIPTOFILE'}=$file;
    } else {
      my $tmp;
      eval {
	require IO::Zlib;
	$tmp = new IO::Zlib();
      } && $tmp || return;
      $tmp->open($file,"rb") || return;
      my $buffer;
      my $length = 1024*1024;
      while (read($tmp,$buffer,$length)) {
	$fh->print($buffer);
      }
      $tmp->close();
      seek($fh,0,'SEEK_SET');
    }
    return $fh;
  } else {
    $fh = new IO::File();
    $fh->open($file,$rw) || return;
  }
  return $fh;
}

sub callback {
  my $callback = shift;
  if (ref($callback) eq 'CODE') {
    return $callback->(@_);
  } elsif (ref($callback) eq 'ARRAY') {
    my ($cb,@args)=@{$callback};
    $cb->(@args,@_);
  }
}

sub _fetch_file {
  my ($uri) = @_;
  my $proto = get_protocol($uri);
  if ($proto eq 'file') {
    my $file = get_filename($uri);
    print STDERR "IOBackend: _fetch_file: $file\n" if $Debug;
    die("File does not exist: $file\n") unless -e $file;
    die("File is not readable: $file\n") unless -r $file;
    die("File is empty: $file\n") if -z $file;
    return ($file,0);
  } elsif ($proto eq 'ntred' or $proto =~ /$reject_proto/) {
    return ($uri,0);
  } elsif (exists($input_protocol_handler{$proto})) {
    my ($new_uri,$unlink) = callback($input_protocol_handler{$proto},$uri);
    my $new_proto = get_protocol($new_uri);
    if ($new_proto ne $proto) {
      return _fetch_file($new_uri);
    } else {
      return ($new_uri,$unlink);
    }
  } else {
    if ($^O eq 'MSWin32') {
      return fetch_file_win32($uri,$proto);
    } else {
      return fetch_file_posix($uri,$proto);
    }
  }
}

sub fetch_file {
  my ($uri) = @_;
  my ($file,$unlink) = &_fetch_file;
  if (get_protocol($file) eq 'file' and _is_gzip($uri)) {
    my ($fh,$ungzfile) = File::Temp::tempfile("tredgzioXXXXXX",
					      DIR => File::Spec->tmpdir(),
					      UNLINK => 0,
					     );
    die "Cannot create temporary file: $!" unless $fh;
    my $tmp;
    eval {
      require IO::Zlib;
      $tmp = new IO::Zlib();
    } && $tmp || die "Cannot load IO::Zlib: $@";
    $tmp->open($file,"rb") || die "Cannot read $uri ($file)";
    my $buffer;
    my $length = 1024*1024;
    while (read($tmp,$buffer,$length)) {
      $fh->print($buffer);
    }
    $tmp->close();
    $fh->close;
    unlink $file if $unlink;
    return ($ungzfile,1);
  } else {
    return ($file,$unlink);
  }
}


sub fetch_cmd {
  my ($cmd, $filename)=@_;
  print "IOBackend: fetch_cmd: $cmd\n" if $Debug;
  if (system($cmd." > ".$filename)==0) {
    return ($filename,1);
  } else {
    warn "$cmd > $filename failed (code $?): $!\n";
    return $filename,0;
  }
}

sub fetch_with_lwp {
  my ($uri,$fh,$filename)=@_;
  my $status = $lwp_user_agent->get($uri, ':content_file' => $filename);
  if ($status and $status->is_error and $status->code == 401) {
    # unauthorized
    # Got authorization error 401, maybe the nonce is stale, let's try again...
    $status = $lwp_user_agent->get($uri, ':content_file' => $filename);
  }
  if ($status->is_success()) {
    close $fh;
    return ($filename,1);
  } else {
    unlink $fh;
    close $fh;
    die "Error occured while fetching URL $uri $@\n".
      $status->status_line()."\n";
  }
}

sub fetch_file_win32 {
  my ($uri,$proto)=@_;
  my ($fh,$filename) = File::Temp::tempfile("tredioXXXXXX",
					    DIR => File::Spec->tmpdir(),
					    SUFFIX => (_is_gzip($uri) ? ".gz" : ""),
					    UNLINK => 0,
					   );
  print STDERR "Fetching URI $uri as proto $proto to $filename\n" if $Debug;
  if ($proto=~m(^https?|ftp|gopher|news)) {
    return fetch_with_lwp($uri,$fh,$filename);
  }
  return($uri,0);
}

sub fetch_file_posix {
  my ($uri,$proto)=@_;
  print "IOBackend: fetching file using protocol $proto ($uri)\n" if $Debug;
  my ($fh,$tempfile) = File::Temp::tempfile("tredioXXXXXX",
					    DIR => File::Spec->tmpdir(),
					    SUFFIX => (_is_gzip($uri) ? ".gz" : ""),
					    UNLINK => 0,
					   );
  print "IOBackend: tempfile: $tempfile\n" if $Debug;
  if ($proto=~m(^https?|ftp|gopher|news)) {
    return fetch_with_lwp($uri,$fh,$tempfile);
  }
  close($fh);
  if ($ssh and -x $ssh and $proto =~ /^(ssh|fish|sftp)$/) {
    print "IOBackend: using plain ssh\n" if $Debug;
    if ($uri =~ m{^\s*(?:ssh|sftp|fish):(?://)?([^-/][^/]*)(/.*)$}) {
      my ($host,$file) = ($1,$2);
      print "IOBackend: tempfile: $tempfile\n" if $Debug;
      return
	fetch_cmd($ssh." ".$ssh_opts." ".quote_filename($host).
	" /bin/cat ".quote_filename(quote_filename($file)),$tempfile);
    } else {
      die "failed to parse URI for ssh $uri\n";
    }
  }
  if ($kioclient and -x $kioclient) {
    print "IOBackend: using kioclient\n" if $Debug;
    # translate ssh protocol to fish protocol
    if ($proto eq 'ssh') {
      ($uri =~ s{^\s*ssh:(?://)?([/:]*)[:/]}{fish://$1/});
    }
    return fetch_cmd($kioclient." ".$kioclient_opts.
		     " cat ".quote_filename($uri),$tempfile);
  }
  if ($curl and -x $curl and $proto =~ /^(?:https?|ftps?|gopher)$/) {
    return fetch_cmd($curl." ".$curl_opts." ".quote_filename($uri),$tempfile);
  }
  warn "No handlers for protocol $proto\n";
  return ($uri,0);
}

sub open_upload_pipe {
  my ($need_gzip,$user_pipe,$upload_pipe)=@_;
  my $fh;
  $user_pipe="| ".$user_pipe if defined($user_pipe) and $user_pipe !~ /^\|/;
  $user_pipe.=" ";
  my $cmd;
  if ($need_gzip) {
    if (-x $gzip) {
      $cmd = $user_pipe."| $gzip $gzip_opts | $upload_pipe ";
    } else {
      die "Need a functional gzip and zcat to open this file\n";
    }
  } else {
    $cmd = $user_pipe."| $upload_pipe ";
  }
  print "IOBackend: upload: $cmd\n" if $Debug;
  open $fh, $cmd || undef $fh;
  return $fh;
}

sub get_upload_fh_win32 {
  my ($uri,$proto,$userpipe)=@_;
  die "Can't save files using protocol $proto on Windows\n";
}

=pod upload_pipe_posix ($uri, $protocol, $userpipe)

Uploading is different from fetching, since it does not use a
temporary file.  Instead, a filehandle to an uploading pipeline is
returned.

=cut

sub get_upload_fh_posix {
  my ($uri,$proto,$userpipe)=@_;
  print "IOBackend: uploading file using protocol $proto ($uri)\n" if $Debug;
  return if $proto eq 'http' or $proto eq 'https';
  if ($ssh and -x $ssh and $proto =~ /^(ssh|fish|sftp)$/) {
    print "IOBackend: using plain ssh\n" if $Debug;
    if ($uri =~ m{^\s*(?:ssh|sftp|fish):(?://)?([^-/][^/]*)(/.*)$}) {
      my ($host,$file) = ($1,$2);
      return open_upload_pipe(_is_gzip($uri), $userpipe, "$ssh $ssh_opts ".
		       quote_filename($host)." /bin/cat \\> ".
			      quote_filename(quote_filename($file)));
    } else {
      die "failed to parse URI for ssh $uri\n";
    }
  }
  if ($kioclient and -x $kioclient) {
    print "IOBackend: using kioclient\n" if $Debug;
    # translate ssh protocol to fish protocol
    if ($proto eq 'ssh') {
      $uri =~ s{^\s*ssh:(?://)?([/:]*)[:/]}{fish://$1/};
    }
    return open_upload_pipe(_is_gzip($uri),$userpipe,
		     "$kioclient $kioclient_opts put ".quote_filename($uri));
  }
  if ($curl and -x $curl and $proto =~ /^(?:ftps?)$/) {
    return open_upload_pipe("$curl --upload-file - $curl_opts ".quote_filename($uri));
  }
  die "No handlers for protocol $proto\n";
}

sub get_store_fh {
  my ($uri,$user_pipe) = @_;
  my $proto = get_protocol($uri);
  if ($proto eq 'file') {
    $uri = get_filename($uri);
    if ($user_pipe) {
      return open_pipe($uri,'w',$user_pipe);
    } else {
      return open_file($uri,'w');
    }
  } elsif ($proto eq 'ntred' or $proto =~ /$reject_proto/) {
    return $uri;
  } else {
    if ($^O eq 'MSWin32') {
      return get_upload_fh_win32($uri,$proto,$user_pipe);
    } else {
      return get_upload_fh_posix($uri,$proto,$user_pipe);
    }
  }
}

sub unlink_uri {
  ($^O eq 'MSWin32') ? &unlink_uri_win32 : &unlink_uri_posix;
}

sub unlink_uri_win32 {
  my ($uri) = @_;
  my $proto = get_protocol($uri);
  if ($proto eq 'file') {
    unlink get_filename($uri);
  } else {
    die "Can't unlink file $uri\n";
  }
}

sub unlink_uri_posix {
  my ($uri)=@_;
  my $proto = get_protocol($uri);
  if ($proto eq 'file') {
    return unlink get_filename($uri);
  }
  print "IOBackend: unlinking file $uri using protocol $proto\n" if $Debug;
  if ($ssh and -x $ssh and $proto =~ /^(ssh|fish|sftp)$/) {
    print "IOBackend: using plain ssh\n" if $Debug;
    if ($uri =~ m{^\s*(?:ssh|sftp|fish):(?://)?([^-/][^/]*)(/.*)$}) {
      my ($host,$file) = ($1,$2);
      return (system("$ssh $ssh_opts ".quote_filename($host)." /bin/rm ".
		     quote_filename(quote_filename($file)))==0) ? 1 : 0;
    } else {
      die "failed to parse URI for ssh $uri\n";
    }
  }
  if ($kioclient and -x $kioclient) {
    # translate ssh protocol to fish protocol
    if ($proto eq 'ssh') {
      $uri =~ s{^\s*ssh:(?://)?([/:]*)[:/]}{fish://$1/};
    }
    return (system("$kioclient $kioclient_opts rm ".quote_filename($uri))==0 ? 1 : 0);
  }
  die "No handlers for protocol $proto\n";
}

sub rename_uri {
  print "IOBackend: rename @_\n" if $Debug;
  ($^O eq 'MSWin32') ? &rename_uri_win32 : &rename_uri_posix;
}


sub rename_uri_win32 {
  my ($uri1,$uri2) = @_;
  my $proto1 = get_protocol($uri1);
  my $proto2 = get_protocol($uri2);
  if ($proto1 eq 'file' and $proto2 eq 'file') {
    my $uri1 = get_filename($uri1);
    return unless -f $uri1;
    rename $uri1, get_filename($uri2);
  } else {
    die "Can't rename file $uri1 to $uri2\n";
  }
}

sub rename_uri_posix {
  my ($uri1,$uri2) = @_;
  my $proto = get_protocol($uri1);
  my $proto2 = get_protocol($uri2);
  if ($proto ne $proto2) {
    die "Can't rename file $uri1 to $uri2\n";
  }
  if ($proto eq 'file') {
    my $uri1 = get_filename($uri1);
    return unless -f $uri1;
    return rename $uri1, get_filename($uri2);
  }
  print "IOBackend: rename file $uri1 to $uri2 using protocol $proto\n" if $Debug;
  if ($ssh and -x $ssh and $proto =~ /^(ssh|fish|sftp)$/) {
    print "IOBackend: using plain ssh\n" if $Debug;
    if ($uri1 =~ m{^\s*(?:ssh|sftp|fish):(?://)?([^-/][^/]*)(/.*)$}) {
      my ($host,$file) = ($1,$2);
      if ($uri2 =~ m{^\s*(?:ssh|sftp|fish):(?://)?([^-/][^/]*)(/.*)$} and $1 eq $host) {
	my $file2 = $2;
	return (system("$ssh $ssh_opts ".quote_filename($host)." /bin/mv ".
		       quote_filename(quote_filename($file))." ".
		       quote_filename(quote_filename($file2)))==0) ? 1 : 0;
      } else {
	die "failed to parse URI for ssh $uri2\n";
      }
    } else {
      die "failed to parse URI for ssh $uri1\n";
    }
  }
  if ($kioclient and -x $kioclient) {
    # translate ssh protocol to fish protocol
    if ($proto eq 'ssh') {
      $uri1 =~ s{^\s*ssh:(?://)?([/:]*)[:/]}{fish://$1/};
      $uri2 =~ s{^\s*ssh:(?://)?([/:]*)[:/]}{fish://$1/};
    }
    return (system("$kioclient $kioclient_opts mv ".quote_filename($uri1).
		     " ".quote_filename($uri2))==0 ? 1 : 0);
  }
  die "No handlers for protocol $proto\n";
}



=item open_backend (filename,mode,encoding?)

Open given file for reading or writing (depending on mode which may be
one of "r" or "w"); Return the corresponding object based on
File::Handle class. Only files the filename of which ends with '.gz'
are considered to be gz-commpressed. All other files are opened using
IO::File.

Optionally, in perl ver. >= 5.8, you may also specify file character
encoding.

=cut


sub open_backend {
  my ($filename, $rw,$encoding)=@_;
  $filename =~ s/^\s*|\s*$//g;
  if ($rw eq 'r') {
    return set_encoding(open_file($filename,$rw)||undef,$encoding);
  } elsif ($rw eq 'w') {
    return set_encoding(get_store_fh($filename)||undef,$encoding);
  } else {
    croak "2nd argument to open_backend must be 'r' or 'w'!";
  }
  return;
}

=pod

=item close_backend (filehandle)

Close given filehandle opened by previous call to C<open_backend>

=cut

sub close_backend {
  my ($fh)=@_;
  # Win32 hack:
  if (ref($fh) eq 'File::Temp') {
    my $filename = ${*$fh}{'ZIPTOFILE'};
    if ($filename ne "") {
      print "IOBackend: Doing the real save to $filename\n" if $Debug;
      seek($fh,0,'SEEK_SET');
      require IO::Zlib;
      my $tmp = new IO::Zlib();
      $tmp->open($filename,"wb") || die "Cannot write to $filename: $!\n";
      # probably bug in Perl 5.8.9? - using just :raw here is not enough
      binmode $fh, ':raw:perlio:bytes';
      local $/;
      $tmp->print(<$fh>);
      $tmp->close;
    }
  }
  my $ret;
  if (UNIVERSAL::isa($fh,'IO::Zlib')) {
    $ret = 1;
  } else {
    $ret = ref($fh) && $fh->close();
  }
  my $unlink = delete $UNLINK_ON_CLOSE{ $fh };
  if ($unlink) {
    unlink $unlink;
  }
  return $ret;
}


=item open_uri (URI,encoding?)

Open given URL for reading, returning an object based on File::Handle
class. Since for some types of URLs this function first copies the
data into a temporary file, use close_uri($fh) on the resulting
filehandle to close it and clean up the temporary file.

Optionally, in perl ver. >= 5.8, you may also specify file character
encoding.

=cut

sub open_uri {
  my ($uri,$encoding) = @_;
  my ($local_file, $is_temporary) = fetch_file( $uri );
  my $fh = IOBackend::open_backend($local_file,'r') || return;
  if ($is_temporary and $local_file ne $uri ) {
    if (!unlink($local_file)) {
      $UNLINK_ON_CLOSE{ $fh } = $local_file;
    }
  }
  return set_encoding($fh,$encoding);
}

*close_uri = \&close_backend;

sub copy_uri {
  my ($src_uri,$target_uri)=@_;
  my $in = open_uri($src_uri)
    or die "Cannot open source $src_uri: $!\n";
  my $out = IOBackend::open_backend($target_uri,'w')
    or die "Cannot open target $target_uri: $!\n";
  my $L=1024*100;
  my $buffer;
  while(read($in,$buffer,$L)>0) {
    print $out ($buffer);
  }
  close_backend($in);
  close_backend($out);
}

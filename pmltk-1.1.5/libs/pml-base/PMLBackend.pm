package PMLBackend;

use Fslib;
use IOBackend qw(close_backend);
use strict;
use warnings;

use PMLInstance qw( :all :diagnostics $DEBUG );

use constant EMPTY => q{};

use Carp;

use vars qw(@pmlformat @pmlpatterns $pmlhint $encoding $config $config_file $allow_no_trees $config_inc_file $TRANSFORM @EXPORT_OK);

use Exporter qw(import);

BEGIN {
  $TRANSFORM=0;
  @EXPORT_OK = qw(open_backend close_backend test read write);
  $encoding='utf-8';
  @pmlformat = ();
  @pmlpatterns = ();
  $pmlhint=EMPTY;
  $config = undef;
  $config_file = 'pmlbackend_conf.xml';
  $config_inc_file = 'pmlbackend_conf.inc';
  $allow_no_trees = 0;
}

sub _caller_dir {
  return File::Spec->catpath(
    (File::Spec->splitpath( (caller)[1] ))[0,1]
  );
}

sub configure {
  return 0 unless eval {
    require XML::LibXSLT;
  };
  undef $config;
  my @resource_path = Fslib::ResourcePaths();
  Fslib::AddResourcePath(_caller_dir());
  my $file = Fslib::FindInResources($config_file,{strict=>1});
  if ($file and -f $file) {
    _debug("config file: $file");
    $config = PMLInstance->load({filename => $file});
  }
  return unless $config;
  my @config_files = Fslib::FindInResources($config_inc_file,{all=>1});
  my $T = $config->get_root->{transform_map} ||= Fslib::Seq->new;
  for my $file (reverse @config_files) {
    _debug("config include file: $file");
    eval {
      my $c = PMLInstance->load({filename => $file});
      # merge
      my $t = $c->get_root->{transform_map};
      if ($t) {
	for my $transform (reverse $t->elements) {
	  $T->unshift_element_obj(Fslib::CloneValue($transform));
	}
      }
    };
    warn $@ if $@;
  }
  Fslib::SetResourcePaths(@resource_path);
  return $config;
}


###################

=item open_backend (filename,mode)

Only reading is supported now!

=cut

sub open_backend {
  my ($filename, $mode, $encoding)=@_;
  my $fh = IOBackend::open_backend($filename,$mode) # discard encoding
    || die "Cannot open $filename for ".($mode eq 'w' ? 'writing' : 'reading').": $!";
  return $fh;
}


=pod

=item read (handle_ref,fsfile)

=cut

sub read ($$) {
  my ($input, $fsfile)=@_;
  return unless ref($fsfile);

  my $ctxt = PMLInstance->load({fh => $input, filename => $fsfile->filename, config => $config });
  $ctxt->convert_to_fsfile( $fsfile );
  my $status = $ctxt->get_status;
  if ($status and 
      !($allow_no_trees or defined($ctxt->get_trees))) {
    _die("No trees found in the PMLInstance!");
  }
  return $status
}


=pod

=item write (handle_ref,fsfile)

=cut

sub write {
  my ($fh,$fsfile)=@_;
  my $ctxt = PMLInstance->convert_from_fsfile( $fsfile );
  $ctxt->save({ fh => $fh, config => $config });
}


=pod

=item test (filehandle | filename, encoding?)

=cut

sub test {
  my ($f,$encoding)=@_;
  if (ref($f)) {
    local $_;
    if ($TRANSFORM and $config) {
      1 while ($_=$f->getline() and !/\S/);
      # see <, assume XML
      return 1 if (defined and /^\s*</);
    } else {
      # only accept PML instances
      # xmlns:...="..pml-namespace.." must occur in the first tag (on one line)
      my ($in_first_tag,$in_pi,$in_comment);
      while ($_=$f->getline()) {
	next if !/\S/;  # whitespace
	if ($in_first_tag) {
	  last if />/;
	  return 1 if m{\bxmlns(?::[[:alnum:]]+)?=([\'\"])http://ufal.mff.cuni.cz/pdt/pml/\1};
	  next;
	} elsif ($in_pi) {
	  next unless s/^.*?\?>//;
	  $in_pi=0;
	} elsif ($in_comment) {
	  next unless s/^.*?\-->//;
	  $in_comment=0;
	}
	s/^(?:\s*<\?.*?\?>|\s*<!--.*?-->)*\s*//;
	if (/<\?/) {
	  $in_pi=1;
	} elsif (/<!--/) {
	  $in_comment=1;
	} elsif (/^</) {
	  last if />/;
	  $in_first_tag=1;
	  return 1 if m{^[^>]*xmlns(?::[[:alnum:]]+)?=([\'\"])http://ufal.mff.cuni.cz/pdt/pml/\1};
	} elsif (length) {
	  return 0; # nothing else allowed before the first tag
	}
      }
      return 0 if !$in_first_tag && !(defined($_) and s/^\s*<//);
      return 1 if defined($_) and m{^[^>]*xmlns(?::[[:alnum:]]+)?=([\'\"])http://ufal.mff.cuni.cz/pdt/pml/\1};
      return 0;
    }
  } else {
    my $fh = IOBackend::open_backend($f,"r");
    my $test = $fh && test($fh,$encoding);
    IOBackend::close_backend($fh);
    return $test;
  }
}


######################################################


################### 
# INIT
###################
package PMLBackend;
eval {
  configure();
};
Carp::cluck( $@ ) if $@;

1;

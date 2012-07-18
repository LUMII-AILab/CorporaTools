package PMLTransformBackend;

use PMLBackend qw(open_backend close_backend read write);

sub test {
  local $PMLBackend::TRANSFORM=1;
  return &PMLBackend::test;
}

1;

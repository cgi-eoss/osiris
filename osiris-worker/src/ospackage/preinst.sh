# check that owner group exists
if ! getent group osiris &>/dev/null ; then
  groupadd osiris
fi

# check that user exists
if ! getent passwd osiris &>/dev/null ; then
  useradd --system --gid osiris osiris
fi

# (optional) check that user belongs to group
if ! id -G -n osiris | grep -qF osiris ; then
  usermod -a -G osiris osiris
fi

# Make application binary mutable if it already exists (i.e. this is a package upgrade)
if test -f /var/osiris/worker/osiris-worker.jar ; then
    chattr -i /var/osiris/worker/osiris-worker.jar
fi

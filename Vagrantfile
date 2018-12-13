# -*- mode: ruby -*-
# vi: set ft=ruby :

# This Vagrantfile may require the following vagrant plugins:
# * vagrant-vbguest (for shared folders in the centos/7 base image)
# * vagrant-puppet-install (for the Puppet provisioner)
#
# They may be installed with "vagrant plugin install <plugin>"

Vagrant.configure('2') do |config|

  config.vm.define 'build', primary: false, autostart: false do |build|
    build.ssh.username = 'osiris'
    build.ssh.password = 'osiris'
    build.vm.synced_folder '.', '/home/osiris/build'
    build.vm.synced_folder `echo $HOME`.chomp + '/.gradle', '/home/osiris/.gradle', create: true

    build.vm.provider 'docker' do |d|
      d.build_dir = './build'
      d.build_args = ['--build-arg=http_proxy', '--build-arg=https_proxy', '--build-arg=no_proxy']
      # Change the internal 'osiris' uid to the current user's uid, and launch sshd
      d.cmd = ['/usr/sbin/sshdBootstrap.sh', `id -u`.chomp, `id -g`.chomp, `stat -c %g /var/run/docker.sock`.chomp, '/usr/sbin/sshd', '-D', '-e']
      d.has_ssh = true
      d.create_args = ['--group-add='+`stat -c %g /var/run/docker.sock`.chomp]
      d.volumes = ['/var/run/docker.sock:/var/run/docker.sock:rw']
    end
  end

  # The default box is an integration testing environment, installing the
  # distribution and configuring with the Puppet manifest.
  config.vm.define 'osiris', primary: true do |osiris|
    osiris.vm.box = 'centos/7'

    # Expose the container's web server on 8080
    osiris.vm.network 'forwarded_port', guest: 80, host: 8080, auto_correct: true # apache
    #osiris.vm.network 'forwarded_port', guest: 5432, host: 5432 # postgresql
    #osiris.vm.network 'forwarded_port', guest: 6565, host: 6565 # osiris-server grpc
    #osiris.vm.network 'forwarded_port', guest: 6566, host: 6566 # osiris-worker grpc
    #osiris.vm.network 'forwarded_port', guest: 6567, host: 6567 # osiris-zoomanager grpc
    #osiris.vm.network 'forwarded_port', guest: 8761, host: 8761 # osiris-serviceregistry http
    #osiris.vm.network 'forwarded_port', guest: 12201, host: 12201 # graylog gelf tcp

    # Create a private network, which allows host-only access to the machine
    # using a specific IP.
    # config.vm.network "private_network", ip: "192.168.33.10"

    # Create a public network, which generally matched to bridged network.
    # Bridged networks make the machine appear as another physical device on
    # your network.
    # config.vm.network "public_network"

    # Ensure the virtualbox provider uses shared folders and not rsynced
    # folders (which may be confused by symlinks)
    osiris.vm.provider 'virtualbox' do |vb|
      osiris.vm.synced_folder '.', '/vagrant', type: 'virtualbox'
      vb.memory = 2048
      vb.cpus = 2
    end

    # Generate yum repo metadata
    ftep.vm.provision 'shell', inline: <<EOF
[ -x /usr/bin/createrepo ] || yum install -y createrepo

createrepo --output=/vagrant/.dist/repo /vagrant/.dist/repo
EOF

    # Puppet provisioning
    #
    # Configure the local environment by editing distribution/puppet/hieradata/standalone.local.yaml
    #
    osiris.puppet_install.puppet_version = '5.5.7'

    # Install r10k to pull in the dependency modules
    osiris.vm.provision 'shell', inline: <<EOF
/opt/puppetlabs/puppet/bin/gem install --quiet r10k

/opt/puppetlabs/puppet/bin/r10k -v info\
  puppetfile install\
  --moduledir /tmp/vagrant-puppet/environments/puppet/modules\
  --puppetfile /tmp/vagrant-puppet/environments/puppet/Puppetfile
EOF

    # Use Vagrant's "puppet apply" provisioning
    osiris.vm.provision 'puppet' do |puppet|
      puppet.environment_path = '.dist'
      puppet.environment = 'puppet'
      puppet.hiera_config_path = '.dist/puppet/hiera.yaml'
      puppet.working_directory = '/tmp/vagrant-puppet/environments/puppet'
      # puppet.options = "--debug"
    end
  end
end

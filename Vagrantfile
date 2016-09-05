# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.require_version ">= 1.8.3"

Vagrant.configure(2) do |config|
  config.vm.box = "boxcutter/ubuntu1604"

  # Use this script to set up and compile the Lila installation. We set
  # `privileged` to `false` because otherwise the provisioning script will run
  # as root. This isn't a problem to install packages globally (`apt-get
  # install`), but `sbt publish-local` will publish to `root`'s home directory!
  # Then we would not be able to use those packages when logged in as
  # `vagrant`.
  config.vm.provision "shell", path: "bin/provision-vagrant.sh", privileged: false

  # IP address to use to connect to the virtual machine. This should be an
  # entry in your hosts file. We use a static IP so that the developer doesn't
  # have to keep adding new entries to their hosts file.
  config.vm.network "private_network", ip: "192.168.34.34"

  # From https://stefanwrobel.com/how-to-make-vagrant-performance-not-suck. You
  # may want to set `cpus` and `mem` yourself.
  config.vm.provider "virtualbox" do |v|
    host = RbConfig::CONFIG['host_os']

    # Fraction of memory of host OS to allocate to VM. More is better!
    memory_fraction = 0.5

    # Give VM allocated system memory & access to all cpu cores on the host
    if host =~ /darwin/
      cpus = `sysctl -n hw.physicalcpu`.to_i
      # sysctl returns Bytes and we need to convert to MB
      mem = `sysctl -n hw.memsize`.to_i / 1024 / 1024
    elsif host =~ /linux/
      cpus = `nproc`.to_i
      # meminfo shows KB and we need to convert to MB
      mem = `grep 'MemTotal' /proc/meminfo | sed -e 's/MemTotal://' -e 's/ kB//'`.to_i / 1024
    else # sorry Windows folks, I can't help you
      cpus = 2
      mem = 4096
    end

    mem *= memory_fraction
    mem = mem.to_i

    # Needed to use multiple CPUs.
    v.customize ["modifyvm", :id, "--ioapic", "on"]

    v.customize ["modifyvm", :id, "--cpus", cpus]
    v.customize ["modifyvm", :id, "--memory", mem]
  end
end

#!/usr/bin/ruby
#This is a fairly horrible script to automatically back up goat
#it is driven by a cron job and saves goat to lagash if there have been
#any changes to him since the last backup

Dir.chdir "/home/bc/src/goatbak/"
`svn update`
version = `svn -R info |grep Revision |sed 's/Revision: //'|sort |tail -n 1`.chomp
Dir.chdir "/home/bc/src/goatbak/tools"
bak_version = nil
bakFile = "bakFile"
begin
    file = File.open(bakFile, "r")
    bak_version = file.gets.chomp
    file.close
rescue
    bak_version = 0    
end

if Integer(bak_version) < Integer(version)
    #We have a new version of goat, time to back him up..
    puts "Getting files from repository."
    `svnadmin dump -q /usr/local/svn/goat/ >goat-r#{version}`
    puts "Compressing goat."
    `bzip2 -9 goat-r#{version}`
    puts "Transferring compressed back up file to remote host."
    `scp goat-r#{version}.bz2 bc@lagash.satanosphere.com:/home/bc/`
    puts "Backed up goat version r#{version}!"
    File.delete("goat-r#{version}.bz2")

    if( $? == 0 )
        batchFile = File.open("batchFile", "w")
        puts "rm goat-r#{bak_version}.bz2"
        batchFile.write("rm goat-r#{bak_version}.bz2")
        batchFile.close
        `sftp -b batchFile bc@lagash.satanosphere.com:/home/bc`
        File.delete("batchFile")
    else
        puts "ERROR backing up goat - likely net is down"
        exit(1)
    end

    #store version
    File.delete(bakFile) if File.exists?(bakFile)
    file = File.open(bakFile, "w")
    file.write(version)
    file.close
else
    puts "Goat up to date - Nothing to do!"
end


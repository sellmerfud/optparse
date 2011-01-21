require 'rake/clean'

task :default => [:build]

BASE_PATH       = '.'
JAR_FILE        = 'optparse.jar'
SRC_DIR         = 'src'
SCALA_SRC_DIR   = "#{SRC_DIR}/scala"
SCALA_FILES     = FileList["#{SCALA_SRC_DIR}/**/*.scala"]

TEST_SCALA_SRC_DIR   = 'test/scala'
TEST_SRC       = FileList["#{TEST_SCALA_SRC_DIR}/**/*.scala"]
SCALATEST_JARS = FileList["test/lib/*.jar"].join(File::PATH_SEPARATOR)
        
                
CLASSES_DIR     = 'classes'
LIB_DIR         = 'lib'
JAR_FILES       = FileList["#{LIB_DIR}/**/*.jar"]
CLASSPATH       = JAR_FILES.join(File::PATH_SEPARATOR)
TEST_CLASSPATH = if CLASSPATH.empty? 
  SCALATEST_JARS + ':classes'
else
  SCALATEST_JARS + File::PATH_SEPARATOR + CLASSPATH + ':classes'
end

RESOURCE_SRC_DIR = "#{SRC_DIR}/resources"
RESOURCE_SRC     = FileList["#{RESOURCE_SRC_DIR}/**/*.xml", "#{RESOURCE_SRC_DIR}/**/*.properties"]
RESOURCE_DEST    = RESOURCE_SRC.sub(/^#{RESOURCE_SRC_DIR}/, CLASSES_DIR)

CLEAN.include(CLASSES_DIR)

# Task to create the classes dir.
directory CLASSES_DIR

def copyAll(src_dir, dest_dir)
  mkdir_p dest_dir unless File.exists?(dest_dir)
  FileList["#{src_dir}/**/*"].each do |src|
    target = src.sub(/^#{src_dir}/, dest_dir)
    mkdir_p File.dirname(target) unless File.exists?(File.dirname(target))
    cp src, target unless File.directory?(src)
  end
end

desc "Copy resource files to the classes directory"
task :resources => [CLASSES_DIR] do
  copyAll(RESOURCE_SRC_DIR, CLASSES_DIR)
end

CLASSPATH_ARG = CLASSPATH.empty? ? "" : "-classpath #{CLASSPATH} "
COMPILE_ARGS = "-sourcepath #{SCALA_SRC_DIR} -d #{CLASSES_DIR} #{CLASSPATH_ARG} #{SCALA_FILES}"

desc "Compile Scala source files"
task :fsc, [:params] => CLASSES_DIR do |t, args|
  case args[:params]
  when '-reset' then sh "fsc -reset"
  when '-shutdown' then sh "fsc -shutdown"
  else sh "fsc #{args[:params]} #{COMPILE_ARGS}"
  end
end
task :compile => :fsc 


desc "Build the project"
task :build => [:compile, :resources] 

desc "Clean, then build the project"
task :clean_build => [:clean, :build]

desc "Create a jar file with the contents of the classes directory"
task :jar => [:build] do
  sh "jar -cf #{JAR_FILE} -C #{CLASSES_DIR} ."
end

TEST_SCALAC_ARGS = "-sourcepath #{TEST_SCALA_SRC_DIR} -d #{CLASSES_DIR} -classpath #{TEST_CLASSPATH} #{TEST_SRC}"
desc "Compile Scala Tests"
task :test_compile do 
  sh "fsc -deprecation #{TEST_SCALAC_ARGS}"
end

desc "Build the unit tests"
task :test_build => [:build, :test_compile]

# The target can be one of:
#  Name of a specific testing spec with or without the Spec suffix
#  Name of a package (All tests in the package will be selected)
def resolveTestTarget(target)
  src = target.gsub(/\./, '/')
  # Try it as is first to see if it is a directory
  dir = FileList["#{CLASSES_DIR}/org/**/#{src}" ].first
  if (dir && File.directory?(dir))
    FileList[dir + '/**/*Spec.class'].map do |s|
      s.sub(/^classes\//,'').sub(/\.class$/, '').gsub('/', '.')
    end.join(' -s ')
  else
    # Try to resolve a specify spec
    spec = FileList["#{CLASSES_DIR}/org/**/"+src.sub(/Spec$/, '') + 'Spec.class'].first
    if spec
      spec.sub(/^classes\//,'').sub(/\.class$/, '').gsub('/', '.')
    else
      puts "Cannot find spec for: #{target}"
      exit 1
    end
  end
end
desc "Run unit tests. Omit [specs] to run all unit tests."
task :test, [:specs] => [:test_build]  do |t, args|
  specs = ''
  specs = "-s #{args[:specs].split(/[\s:]+/).map { |s| resolveTestTarget(s) }.join(' -s ')}" if args[:specs]
  sh "scala -cp #{TEST_CLASSPATH} org.scalatest.tools.Runner -p #{CLASSES_DIR} -o #{specs}"
end


#  Copyright (c) 2011 Curt Sellmer
#  
#  Permission is hereby granted, free of charge, to any person obtaining
#  a copy of this software and associated documentation files (the
#  "Software"), to deal in the Software without restriction, including
#  without limitation the rights to use, copy, modify, merge, publish,
#  distribute, sublicense, and/or sell copies of the Software, and to
#  permit persons to whom the Software is furnished to do so, subject to
#  the following conditions:
#  
#  The above copyright notice and this permission notice shall be
#  included in all copies or substantial portions of the Software.
#  
#  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
#  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
#  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
#  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
#  LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
#  OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
#  WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

require 'rake/clean'

task :default => [:build]

BASE_PATH       = '.'
JAR_FILE        = 'optparse.jar'
SRC_DIR         = 'src'
SCALA_SRC_DIR   = "#{SRC_DIR}/scala"
SCALA_FILES     = FileList["#{SCALA_SRC_DIR}/**/*.scala"]

TEST_SCALA_SRC_DIR = 'test/scala'
TEST_SRC       = FileList["#{TEST_SCALA_SRC_DIR}/**/*.scala"]
SCALATEST_JARS = FileList["test/lib/*.jar"].join(File::PATH_SEPARATOR)
        
                
CLASSES_DIR    = 'classes'
TEST_CLASSPATH =  SCALATEST_JARS + ':classes'

DOC_DIR = 'scaladoc'

CLEAN.include(CLASSES_DIR)
CLOBBER.include(JAR_FILE, DOC_DIR)

directory CLASSES_DIR

COMPILE_ARGS = "-sourcepath #{SCALA_SRC_DIR} -d #{CLASSES_DIR} #{SCALA_FILES}"

desc "Compile scala source files (param can be -reset or -shutdown)."
task :fsc, [:param] => CLASSES_DIR do |t, args|
  case args[:param]
  when '-reset' then sh "fsc -reset"
  when '-shutdown' then sh "fsc -shutdown"
  else sh "fsc #{args[:params]} #{COMPILE_ARGS}"
  end
end
desc "Compile scala source files (same as fsc)."
task :compile => :fsc 


desc "Build the jar file and the api documentation."
task :build => [:jar, :doc] 

desc "Create a jar file with the contents of the classes directory."
task :jar => [:compile] do
  sh "jar -cf #{JAR_FILE} -C #{CLASSES_DIR} ."
end

directory DOC_DIR

desc "Create api documentation."
task :doc => [DOC_DIR] do
  sh "scaladoc -d #{DOC_DIR} -doc-title 'Option Parser' #{SCALA_FILES}"
end


TEST_SCALAC_ARGS = "-sourcepath #{TEST_SCALA_SRC_DIR} -d #{CLASSES_DIR} -classpath #{TEST_CLASSPATH} #{TEST_SRC}"
desc "Compile unit tests."
task :test_compile do 
  sh "fsc -deprecation #{TEST_SCALAC_ARGS}"
end

desc "Build the unit tests."
task :test_build => [:compile, :test_compile]

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

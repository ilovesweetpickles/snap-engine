import sys
import sysconfig
import os
import os.path
import platform
import argparse
import zipfile
import logging


def _configure_beampy(snap_home=None,
                      java_module=None,
                      java_home=None,
                      req_arch=None,
                      req_java=False,
                      req_py=False,
                      force=False):
    """
    Unzips matching jpy binary distribution from ../lib/jpy.<platform>-<python-version>.zip,
    imports unpacked 'jpyutil' and configures jpy for BEAM.

    :param snap_home: SNAP distribution directory.
    :param java_home: Java home directory. See also Java system property "java.home".
    :param req_arch:  Required JVM architecture (amd64, ia86, x86, etc). See Java system property "os.arch".
    :param req_java:  Fail, if configuration of jpy's Java API fails.
    :param req_py:    Fail, if configuration of jpy's Python API fails.
    :param force:     Force overwriting of existing files.
    :return:
    """

    ret_code = 0

    logging.info("installing from Java module '" + java_module + "'")

    if req_arch:
        req_arch = req_arch.lower()
        act_arch = platform.machine().lower()
        if req_arch != act_arch:
            logging.warning("architecture requirement possibly not met: "
                            "Python is " + act_arch + " but JVM requires " + req_arch)
        is64 = sys.maxsize > 2 ** 31 - 1
        if is64 and not req_arch in ('amd64', 'ia64', 'x64', 'x86_64'):
            logging.warning("architecture requirement possibly not met: "
                            "Python is 64 bit but JVM requires " + req_arch)

    beampy_dir = os.path.dirname(os.path.abspath(__file__))
    beampy_ini_file = os.path.join(beampy_dir, 'beampy.ini')
    jpy_distr_name = 'jpy.' + sysconfig.get_platform() + '-' + sysconfig.get_python_version()
    jpy_info_file = os.path.join(beampy_dir, jpy_distr_name + '.info')
    jpyutil_file = os.path.join(beampy_dir, 'jpyutil.py')
    jpyconfig_java_file = os.path.join(beampy_dir, 'jpyconfig.properties')
    jpyconfig_py_file = os.path.join(beampy_dir, 'jpyconfig.py')

    #
    # Write initial beampy.ini. Note, this file is only used if you use SNAP from Python, i.e. importing
    # the beampy module in your Python programs.
    #
    if force or not os.path.exists(beampy_ini_file):
        with open(beampy_ini_file, 'w') as file:
            file.writelines(['[DEFAULT]\n',
                             'snap_home = %s\n' % snap_home,
                             '# extra_classpath: target/classes',
                             '# extra_options: -Djava.awt.headless=false\n',
                             '# max_mem: 4G\n',
                             '# debug: False\n'])


    #
    # Extract a matching jpy binary distribution ..snap-python.jar!/lib/jpy.<platform>-<python-version>.zip
    #
    if os.path.isfile(java_module):
        member = 'lib/' + jpy_distr_name + '.zip'
        logging.info("extracting '" + member + "' from '" + java_module + "'")
        with zipfile.ZipFile(java_module) as zf:
            jpy_archive_file = zf.extract(member, beampy_dir)
    else:
        jpy_archive_file = os.path.join(java_module, 'lib', jpy_distr_name + '.zip')


    #
    # Unpack jpy.<platform>-<python-version>.zip
    #
    if force or not os.path.exists(jpy_info_file):
        if not os.path.exists(jpy_archive_file):
            logging.error("Can't find binary distribution '" + jpy_archive_file + "'")
            logging.error("... of Python module 'jpy' for this system. You can try to generate one yourself.")
            logging.error("... Please go to https://github.com/bcdev/jpy and follow the build instructions")
            logging.error("... given there.")
            return 10

        # print('Found binary distribution ' + archive_path)
        # os.mkdir(os.path.join(basename)
        logging.info("unzipping '" + jpy_archive_file + "'")
        with zipfile.ZipFile(jpy_archive_file) as zf:
            zf.extractall(beampy_dir)

    #
    # Execute jpyutil.py to write runtime configuration:
    # - jpyconfig.properties - Configuration for Java about Python (jpy extension module)
    # - jpyconfig.py - Configuration for Python about Java (JVM)
    #
    if force or \
            not os.path.exists(jpyconfig_java_file) or \
            not os.path.exists(jpyconfig_py_file):
        if os.path.exists(jpyutil_file):
            # Note 'jpyutil.py' has been unpacked by previous step, so we safely import it
            import jpyutil

            if not java_home:
                jre_dir = os.path.join(beampy_dir,
                                       '..',  # --> beam-python-<version>/
                                       '..',  # --> modules/
                                       '..',  # --> ${beam.home}
                                       'jre')  # --> ${beam.home}/jre
                if os.path.exists(jre_dir):
                    java_home = os.path.normpath(jre_dir)

            ret_code = jpyutil.write_config_files(out_dir=beampy_dir,
                                                  java_home_dir=java_home,
                                                  req_java_api_conf=req_java,
                                                  req_py_api_conf=req_py)
        else:
            logging.error("Missing Python module '" + jpyutil_file + "' required to complete the configuration.")
            logging.error("This file should have been part of binary distribution '" + jpy_archive_file + "'.")
            ret_code = 20

    return ret_code


def _main():
    parser = argparse.ArgumentParser(description='Configures beampy, the BEAM Python interface.')
    parser.add_argument('--snap_home', default=None,
                        help='SNAP distribution directory')
    parser.add_argument('--req_arch', default=None,
                        help='required JVM architecture, e.g. "amd64", '
                             'may be taken from Java system property "os.arch"')
    parser.add_argument('--java_module', default=None,
                        help='directory or JAR file containing the "snap-python" Java module')
    parser.add_argument('--java_home', default=None,
                        help='Java JDK or JRE installation directory, '
                             'may be taken from Java system property "java.home"')
    parser.add_argument("--log_file", action='store', default=None, help="file into which to write logging output")
    parser.add_argument("--log_level", action='store', default='INFO',
                        help="log level, possible values are: DEBUG, INFO, WARNING, ERROR")
    parser.add_argument("-j", "--req_java", action='store_true', default=False,
                        help="require that Java API configuration succeeds")
    parser.add_argument("-p", "--req_py", action='store_true', default=False,
                        help="require that Python API configuration succeeds")
    parser.add_argument('-f', '--force', action='store_true', default=False,
                        help='force overwriting of existing files')
    args = parser.parse_args()

    log_level = getattr(logging, args.log_level.upper(), None)
    if not isinstance(log_level, int):
        raise ValueError('Invalid log level: %s' % log_level)

    log_format = '%(levelname)s: %(message)s'
    log_file = args.log_file
    if log_file:
        logging.basicConfig(format=log_format, level=log_level, filename=log_file, filemode='w')
    else:
        logging.basicConfig(format=log_format, level=log_level)

    try:
        ret_code = _configure_beampy(snap_home=args.snap_home,
                                     java_module=args.java_module,
                                     java_home=args.java_home,
                                     req_arch=args.req_arch,
                                     req_java=args.req_java,
                                     req_py=args.req_py,
                                     force=args.force)
    except:
        logging.exception("Configuration failed")
        ret_code = 10

    exit(ret_code)


if __name__ == '__main__':
    _main()

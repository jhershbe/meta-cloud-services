DESCRIPTION = "OAuth 2.0 client library"
AUTHOR = "Google Inc."
HOMEPAGE = "http://github.com/google/oauth2client/"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=038e1390e94fe637991fa5569daa62bc"

inherit setuptools pypi

SRC_URI[md5sum] = "bc30976cd1e859cd6199264a50c5ba72"
SRC_URI[sha256sum] = "bd3062c06f8b10c6ef7a890b22c2740e5f87d61b6e1f4b1c90d069cdfc9dadb5"

RDEPENDS_${PN} += " \
        python-httplib2 \
        python-pyasn1 \
        python-pyasn1-modules \
        python-rsa \
        python-six \
        "

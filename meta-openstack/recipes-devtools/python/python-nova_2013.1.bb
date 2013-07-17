DESCRIPTION = "Nova is a cloud computing fabric controller"
HOMEPAGE = "https://launchpad.net/nova"
SECTION = "devel/python"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=1dece7821bf3fd70fe1309eaa37d52a2"

DEPENDS = "sudo"

PR = "r0"
SRCNAME = "nova"

SRC_URI = "https://launchpad.net/${SRCNAME}/grizzly/${PV}/+download/${SRCNAME}-${PV}.tar.gz \
           file://nova.conf \
           file://nova-compute \
           file://nova-all \
           file://openrc \
           "

SRC_URI[md5sum] = "38022353c398ce38c4e220d1d18b5916"
SRC_URI[sha256sum] = "db7f5259d848358bf14105d5833869ec145f643312e6bc0adef0050120fe3e07"

S = "${WORKDIR}/${SRCNAME}-${PV}"

inherit setuptools useradd update-rc.d identity

do_install_append() {
    TEMPLATE_CONF_DIR=${S}${sysconfdir}/${SRCNAME}
    NOVA_CONF_DIR=${D}/${sysconfdir}/nova

    install -d ${NOVA_CONF_DIR}
    install -m 600 ${S}/etc/nova/policy.json ${NOVA_CONF_DIR}/

    # Deploy filters to /etc/nova/rootwrap.d
    install -m 755 -d ${NOVA_CONF_DIR}/rootwrap.d
    install -m 600 ${S}/etc/nova/rootwrap.d/*.filters ${NOVA_CONF_DIR}/rootwrap.d
    chown -R root:root ${NOVA_CONF_DIR}/rootwrap.d
    chmod 644 ${NOVA_CONF_DIR}/rootwrap.d

     # Set up rootwrap.conf, pointing to /etc/nova/rootwrap.d
    install -m 644 ${S}/etc/nova/rootwrap.conf ${NOVA_CONF_DIR}/
    sed -e "s:^filters_path=.*$:filters_path=${NOVA_CONF_DIR}/rootwrap.d:" \
        -i ${NOVA_CONF_DIR}/rootwrap.conf
    chown root:root $NOVA_CONF_DIR/rootwrap.conf

    # Set up the rootwrap sudoers for nova
    install -d ${D}${sysconfdir}/sudoers.d
    touch ${D}${sysconfdir}/sudoers.d/nova-rootwrap
    chmod 0440 ${D}${sysconfdir}/sudoers.d/nova-rootwrap
    chown root:root ${D}${sysconfdir}/sudoers.d/nova-rootwrap
    echo "root ALL=(root) NOPASSWD: ${bindir}/nova-rootwrap" > \
        ${D}${sysconfdir}/sudoers.d/nova-rootwrap

    #Configuration options
    sed -e "s:%SERVICE_TENANT_NAME%:${SERVICE_TENANT_NAME}:g" \
        ${TEMPLATE_CONF_DIR}/api-paste.ini > ${WORKDIR}/api-paste.ini
    sed -e "s:%SERVICE_USER%:${SRCNAME}:g" -i ${WORKDIR}/api-paste.ini
    sed -e "s:%SERVICE_PASSWORD%:${SERVICE_PASSWORD}:g" \
        -i ${WORKDIR}/api-paste.ini

    sed -e "s:%DB_USER%:${DB_USER}:g" -i ${WORKDIR}/nova.conf
    sed -e "s:%DB_PASSWORD%:${DB_PASSWORD}:g" -i ${WORKDIR}/nova.conf

    sed -e "s:%OS_PASSWORD%:${ADMIN_PASSWORD}:g" -i ${WORKDIR}/openrc
    sed -e "s:%SERVICE_TOKEN%:${SERVICE_TOKEN}:g" -i ${WORKDIR}/openrc

    #Copy the configuration file
    install -m 664 ${WORKDIR}/nova.conf     ${NOVA_CONF_DIR}/nova.conf
    install -m 664 ${WORKDIR}/api-paste.ini ${NOVA_CONF_DIR}
    install -m 664 ${WORKDIR}/openrc        ${NOVA_CONF_DIR}

    install -d ${NOVA_CONF_DIR}/instances

    if ${@base_contains('DISTRO_FEATURES', 'sysvinit', 'true', 'false', d)}; then
        install -d ${D}${sysconfdir}/init.d
        install -m 0755 ${WORKDIR}/nova-compute ${D}${sysconfdir}/init.d/nova-compute
        install -m 0755 ${WORKDIR}/nova-all ${D}${sysconfdir}/init.d/nova-all
    fi

}

USERADD_PACKAGES = "${PN}"
GROUPADD_PARAM_${PN} = "--system nova"
USERADD_PARAM_${PN}  = "--system --home /var/lib/nova -g nova \
                        --no-create-home --shell /bin/false nova"

PACKAGES += "${SRCNAME}-common ${SRCNAME}-compute ${SRCNAME}-controller"

pkg_postinst_${SRCNAME}-common () {
    if [ "x$D" != "x" ]; then
        exit 1
    fi

    echo "source /etc/nova/openrc" > /home/root/.bashrc
}

pkg_postinst_${SRCNAME}-controller () {
    if [ "x$D" != "x" ]; then
        exit 1
    fi

    # This is to make sure postgres is configured and running
    if ! pidof postmaster > /dev/null; then
       sudo -u postgres initdb -D /etc/postgresql/
       /etc/init.d/postgresql start
       sleep 0.2
       sudo -u postgres psql -c "CREATE ROLE ${DB_USER} WITH SUPERUSER LOGIN PASSWORD '${DB_PASSWORD}'"
    fi

    sudo -u postgres createdb nova
    nova-manage db sync
}


FILES_${PN} = "${libdir}/*"

FILES_${SRCNAME}-common = " ${bindir}/nova-manage \
                      ${bindir}/nova-rootwrap \
                      ${sysconfdir}/${SRCNAME}/* \
                      ${sysconfdir}/sudoers.d"

FILES_${SRCNAME}-compute = "${bindir}/nova-compute \
                            ${sysconfdir}/init.d/nova-compute"

FILES_${SRCNAME}-controller = "${bindir}/* \
                               ${sysconfdir}/init.d/nova-all"

RDEPENDS_${PN} = " python-modules \
    python-misc \
    python-argparse \
    python-amqplib \
    python-anyjson \
    python-babel \
    python-boto \
    python-cinderclient \
    python-cheetah \
    python-eventlet \
    python-feedparser \
    python-glanceclient \
    python-greenlet \
    python-httplib2 \
    python-iso8601 \
    python-kombu \
    python-lxml \
    python-netaddr \
    python-oslo.config \
    python-paste \
    python-pastedeploy \
    python-paramiko \
    python-pyasn1 \
    python-setuptools-git \
    python-simplejson \
    python-setuptools \
    python-sqlalchemy \
    python-sqlalchemy-migrate \
    python-stevedore \
    python-suds \
    python-quantumclient \
    python-routes \
    python-webob \
    python-websockify \
    "

RDEPENDS_${SRCNAME}-common = "${PN} openssl openssl-misc libxml2 libxslt \
        iptables curl dnsmasq sudo procps"

RDEPENDS_${SRCNAME}-compute = "${PN} nova-common \
       qemu libvirt libvirt-libvirtd libvirt-python libvirt-virsh"

RDEPENDS_${SRCNAME}-controller = "${PN} nova-common \
        postgresql postgresql-client python-psycopg2"

INITSCRIPT_PACKAGES = "${SRCNAME}-compute ${SRCNAME}-controller"
INITSCRIPT_NAME_${SRCNAME}-compute = "nova-compute"
INITSCRIPT_NAME_${SRCNAME}-controller = "nova-all"

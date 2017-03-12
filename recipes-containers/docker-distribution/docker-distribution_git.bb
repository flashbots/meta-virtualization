HOMEPAGE = "http://github.com/docker/distribution"
SUMMARY = "The Docker toolset to pack, ship, store, and deliver content"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=d2794c0df5b907fdace235a619d80314"

DEPENDS = "go-cross-${TARGET_ARCH} \
          "

SRCREV_distribution="0810eba2adf048b77621472991211924d9ec31c5"
SRC_URI = "git://github.com/docker/distribution.git;branch=master;name=distribution;destsuffix=git/src/github.com/docker/distribution \
           file://docker-registry.service \
          "

PACKAGES =+ "docker-registry"

PV = "v2.6.0-rc+git${SRCPV}"
S = "${WORKDIR}/git/src/github.com/docker/distribution"

inherit go-osarchmap

# This disables seccomp and apparmor, which are on by default in the
# go package. 
EXTRA_OEMAKE="BUILDTAGS=''"

do_compile() {
	export GOARCH="${TARGET_GOARCH}"
	export GOPATH="${WORKDIR}/git/"

	# Pass the needed cflags/ldflags so that cgo
	# can find the needed headers files and libraries
	export CGO_ENABLED="1"
	export CFLAGS=""
	export LDFLAGS=""
	export CGO_CFLAGS="${BUILDSDK_CFLAGS} --sysroot=${STAGING_DIR_TARGET}"
	export CGO_LDFLAGS="${BUILDSDK_LDFLAGS} --sysroot=${STAGING_DIR_TARGET}"

	oe_runmake binaries
}

do_install() {
	install -d ${D}/${sbindir}
	install ${S}/bin/registry ${D}/${sbindir}

	if ${@bb.utils.contains('DISTRO_FEATURES','systemd','true','false',d)}; then
	    install -d ${D}${systemd_unitdir}/system
	    install -m 644 ${WORKDIR}/docker-registry.service ${D}/${systemd_unitdir}/system
	fi

	install -d ${D}/${sysconfdir}/docker-distribution/registry/
	install ${S}/cmd/registry/config-example.yml ${D}/${sysconfdir}/docker-distribution/registry/config.yml

	# storage for the registry containers
	install -d ${D}/${localstatedir}/lib/registry/
}

INSANE_SKIP_${PN} += "ldflags already-stripped"
INSANE_SKIP_docker-registry += "ldflags already-stripped"

FILES_docker-registry = "${sbindir}/*"
FILES_docker-registry += "${systemd_unitdir}/system/docker-registry.service"
FILES_docker-registry += "${sysconfdir}/docker-distribution/*"
FILES_docker-registry += "${localstatedir}/lib/registry/"

SYSTEMD_SERVICE_docker-registry = "${@bb.utils.contains('DISTRO_FEATURES','systemd','docker-registry.service','',d)}"
SYSTEMD_AUTO_ENABLE_docker-registry = "enable"

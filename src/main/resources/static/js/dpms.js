/*
 * D-PMS 공통 화면 스크립트
 * CSP(script-src 'self') 때문에 HTML 안의 onclick/onsubmit/<script> 를 쓰지 않고,
 * data-* 속성을 보고 이 파일에서 동작을 연결한다.
 */
(function () {
    'use strict';

    // ---------- 페이징 (eGov PaginationRenderer 가 그린 data-page 링크) ----------
    // eGov 표준 화면의 fn_egov_link_page(pageNo) 와 같은 역할: 검색 폼의 pageIndex 만 바꿔 다시 조회한다.
    document.addEventListener('click', function (event) {
        var link = event.target.closest('.pagination [data-page]');
        if (!link) {
            return;
        }
        event.preventDefault();
        var page = link.getAttribute('data-page');
        var form = document.getElementById('searchForm');
        if (form && form.elements.pageIndex) {
            form.elements.pageIndex.value = page;
            form.submit();   // submit() 은 submit 이벤트를 발생시키지 않으므로 아래 1페이지 초기화와 충돌하지 않는다
            return;
        }
        var url = new URL(window.location.href);
        url.searchParams.set('pageIndex', page);
        window.location.assign(url.toString());
    });

    // ---------- 폼: 확인 창, 검색 시 1페이지부터, 중복 제출 방지 ----------
    document.addEventListener('submit', function (event) {
        var form = event.target;
        var message = form.getAttribute('data-confirm');
        if (message && !window.confirm(message)) {
            event.preventDefault();
            return;
        }
        if (form.id === 'searchForm' && form.elements.pageIndex) {
            form.elements.pageIndex.value = 1;
        }
        if (!form.hasAttribute('data-allow-resubmit')) {
            form.querySelectorAll('button[type="submit"]').forEach(function (button) {
                button.disabled = true;
                button.setAttribute('aria-busy', 'true');
            });
        }
    });

    // 뒤로 가기로 돌아온 화면(bfcache)에서는 비활성화한 버튼을 되돌린다
    window.addEventListener('pageshow', function () {
        document.querySelectorAll('button[aria-busy="true"]').forEach(function (button) {
            button.disabled = false;
            button.removeAttribute('aria-busy');
        });
    });

    // ---------- 자동 계산 (서버에서 다시 계산하므로 화면 표시는 안내용) ----------
    function numberValue(id) {
        var el = document.getElementById(id);
        var value = el ? parseInt(el.value, 10) : NaN;
        return isNaN(value) ? 0 : value;
    }

    var calculators = {
        'self-funding': function () {
            var target = document.getElementById('selfFunding');
            if (target) {
                target.value = Math.max(0, numberValue('totalBudget') - numberValue('requestGrant'));
            }
        },
        'vat': function (input) {
            var target = document.getElementById(input.getAttribute('data-vat-target'));
            if (target) {
                var supply = parseInt(input.value, 10);
                target.value = isNaN(supply) ? '' : Math.floor(supply / 10);   // 서버와 같이 원 단위 미만 절사
            }
        },
        'score-total': function () {
            var target = document.getElementById('totalScore');
            if (target) {
                target.value = numberValue('quantitativeScore') + numberValue('qualitativeScore');
            }
        }
    };

    document.addEventListener('input', function (event) {
        var name = event.target.getAttribute('data-calc');
        if (name && calculators[name]) {
            calculators[name](event.target);
        }
    });

    document.querySelectorAll('[data-calc]').forEach(function (input) {
        var name = input.getAttribute('data-calc');
        if (calculators[name]) {
            calculators[name](input);
        }
    });

    // ---------- 데모 계정 빠른 입력 (로그인 화면) ----------
    document.querySelectorAll('[data-demo-user]').forEach(function (button) {
        button.addEventListener('click', function () {
            document.getElementById('userId').value = button.getAttribute('data-demo-user');
            document.getElementById('password').value = button.getAttribute('data-demo-password');
            document.getElementById('password').focus();
        });
    });

    // ---------- 이전 화면 ----------
    document.querySelectorAll('[data-action="back"]').forEach(function (button) {
        button.addEventListener('click', function () {
            window.history.back();
        });
    });

    // ---------- 모달 폼 채우기 (정산 보완 재요청) ----------
    // 버튼의 data-fill-* 값을 data-fill-target 모달의 같은 name 입력칸에 넣는다.
    document.querySelectorAll('[data-fill-target]').forEach(function (button) {
        button.addEventListener('click', function () {
            var modal = document.querySelector(button.getAttribute('data-fill-target'));
            if (!modal) {
                return;
            }
            Object.keys(button.dataset).forEach(function (key) {
                if (key.indexOf('fill') !== 0 || key === 'fillTarget') {
                    return;
                }
                var name = key.charAt(4).toLowerCase() + key.slice(5);
                var field = modal.querySelector('[name="' + name + '"]');
                if (field) {
                    field.value = button.dataset[key];
                    field.dispatchEvent(new Event('input', { bubbles: true }));
                }
            });
            var form = modal.querySelector('form');
            if (form && button.getAttribute('data-form-action')) {
                form.setAttribute('action', button.getAttribute('data-form-action'));
            }
        });
    });

    // ---------- 막대 길이 (분야별 예산) ----------
    // 금액은 글자로 이미 표시되어 있고, 여기서는 가장 큰 값을 100% 로 막대 폭만 맞춘다.
    // style 속성 대신 CSSOM(el.style.width)으로 넣으므로 CSP(style-src 'self')에 걸리지 않는다
    document.querySelectorAll('[data-bar-list]').forEach(function (list) {
        var items = list.querySelectorAll('[data-value]');
        var max = 0;
        items.forEach(function (item) {
            max = Math.max(max, Number(item.getAttribute('data-value')) || 0);
        });
        items.forEach(function (item) {
            var fill = item.querySelector('.bar-fill');
            if (fill && max > 0) {
                fill.style.width = ((Number(item.getAttribute('data-value')) || 0) / max * 100) + '%';
            }
        });
    });
})();

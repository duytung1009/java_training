import test from 'node:test';
import assert from 'node:assert/strict';
import { dispVal, valPrefix, itemLabel } from '../../main/resources/static/js/items.js';
import { pathD, corners, wirePath, iso } from '../../main/resources/static/js/map/iso.js';

test('dispVal: item tính ra có tiền tố "= ", trừ khi nhãn đã bắt đầu bằng "="', () => {
  assert.equal(dispVal({ value: 5 }), '5');
  assert.equal(dispVal({ value: 5, computed: true }), '= 5');
  assert.equal(dispVal({ value: 5, computed: true, label: 'phí' }), '= 5');
  assert.equal(dispVal({ value: 5, computed: true, label: '= phí' }), '5');
  assert.equal(dispVal({ value: '', computed: true }), '= ');
});

test('valPrefix: phần đứng trước giá trị của dispVal', () => {
  assert.equal(valPrefix({ value: 5 }), '');
  assert.equal(valPrefix({ value: 5, computed: true }), '= ');
  assert.equal(valPrefix({ value: 5, computed: true, label: '=x' }), '');
});

test('itemLabel: label nếu có, không thì key', () => {
  assert.equal(itemLabel({ key: 'k', label: 'L' }), 'L');
  assert.equal(itemLabel({ key: 'k' }), 'k');
});

test('pathD / wirePath: M rồi L, điểm cách nhau bằng khoảng trắng', () => {
  assert.equal(pathD([[1, 2], [3, 4], [5, 6]]), 'M1 2 L3 4 L5 6');
  assert.equal(pathD([]), '');
  assert.equal(wirePath([[0, 0], [1, 0]]), pathD([iso(0, 0), iso(1, 0)]));
});

test('corners: 4 góc, nhận cả d lẫn h', () => {
  assert.deepEqual(corners({ x: 1, y: 2, w: 3, d: 4 }), [[1, 2], [4, 2], [4, 6], [1, 6]]);
  assert.deepEqual(corners({ x: 0, y: 0, w: 3, h: 4 }), [[0, 0], [3, 0], [3, 4], [0, 4]]);
});
